import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Arrays;
import java.sql.*;

public class decrypt {
    public static void main(String args[]) {
        if (args.length < 3) {
            System.out.println("Error: Faltan argumentos.");
            System.out.println("Ejecuta asi: java decrypt c1.enc MiContrasena foto_recuperada.jpg");
            return;
        }

        try {
            decryptFlow(new File(args[0]), args[1], new File(args[2]));
        } catch (Exception e) {
            System.err.println("Error al descifrar (¿Contraseña incorrecta o archivo no registrado?):");
            e.printStackTrace();
        }
    }

    static void decryptFlow(File archivoCifrado, String passwordUsuario, File archivoDescifrado) throws Exception {
        String algorithm = "AES/GCM/NoPadding";
        
        System.out.println("\n--- RECUPERANDO ARCHIVO DESDE MYSQL ---");
        
        byte[] claveAESProtegida = null;
        byte[] paquetePrivadaFinal = null;

        try (Connection conn = ConexionDB.conectar()) {
            // 1. Cargamos la Clave AES envuelta usando la ruta exacta del archivo físico
            String sqlArchivo = "SELECT clave_aes_cifrada FROM archivos_multimedia WHERE ruta_cifrado = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlArchivo)) {
                pstmt.setString(1, archivoCifrado.getAbsolutePath());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        claveAESProtegida = rs.getBytes("clave_aes_cifrada");
                    } else {
                        throw new Exception("El archivo no existe en la base de datos.");
                    }
                }
            }

            // 2. Cargamos la clave privada cifrada del usuario
            String sqlRsa = "SELECT clave_privada_cifrada FROM credenciales_rsa ORDER BY id DESC LIMIT 1";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlRsa)) {
                if (rs.next()) {
                    paquetePrivadaFinal = rs.getBytes("clave_privada_cifrada");
                } else {
                    throw new Exception("No hay credenciales registradas en la base de datos.");
                }
            }
        }

        // 3. Reconstruimos la clave privada desenredándola con la contraseña del usuario
        // Separamos los 12 bytes del IV y el resto que es la clave cifrada
        byte[] ivPrivada = Arrays.copyOfRange(paquetePrivadaFinal, 0, 12);
        byte[] encryptedPrivateKey = Arrays.copyOfRange(paquetePrivadaFinal, 12, paquetePrivadaFinal.length);

        GCMParameterSpec specPrivada = new GCMParameterSpec(128, ivPrivada);
        SecretKey aesKeyHash = getAESKeyFromPassword(passwordUsuario);
        Cipher cipherPriv = Cipher.getInstance("AES/GCM/NoPadding");
        cipherPriv.init(Cipher.DECRYPT_MODE, aesKeyHash, specPrivada);
        
        byte[] decryptedPrivateKeyBytes = cipherPriv.doFinal(encryptedPrivateKey);
        PrivateKey clavePrivadaRecuperada = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decryptedPrivateKeyBytes));
        
        // 4. Desenvolvemos la clave AES del archivo multimedia
        Cipher unwrapCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        unwrapCipher.init(Cipher.UNWRAP_MODE, clavePrivadaRecuperada);
        SecretKey claveAESRecuperada = (SecretKey) unwrapCipher.unwrap(claveAESProtegida, "AES", Cipher.SECRET_KEY);
        
        System.out.println("Clave AES desenvuelta con éxito.");

        // 5. Desciframos el archivo físico (el IV se lee automáticamente dentro del método)
        System.out.println("Descifrando archivo... Generando -> " + archivoDescifrado.getName());
        decryptFile(algorithm, archivoCifrado, archivoDescifrado, claveAESRecuperada);

        System.out.println("¡Proceso completado con éxito!");
    }

    public static void decryptFile(String algorithm, File inputFile, File outputFile, SecretKey key) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
             
            // Se obtiene el iv del principio del archivo
            byte[] ivBytes = new byte[12];
            int leidos = inputStream.read(ivBytes);
            if (leidos < 12) {
                throw new Exception("El archivo cifrado está corrupto o es demasiado corto.");
            }
            GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) outputStream.write(output);
            }
            byte[] outputBytes = cipher.doFinal();
            if (outputBytes != null) outputStream.write(outputBytes);
        }
    }

    public static SecretKey getAESKeyFromPassword(String password) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes("UTF-8"));
        return new SecretKeySpec(Arrays.copyOf(hash, 16), "AES");
    }
}