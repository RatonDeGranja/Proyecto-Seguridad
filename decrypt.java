import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Arrays;

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
            System.err.println("Error al descifrar (¿Contraseña incorrecta?):");
            e.printStackTrace();
        }
    }

    static void decryptFlow(File archivoCifrado, String passwordUsuario, File archivoDescifrado) throws Exception {
        String algorithm = "AES/GCM/NoPadding";
        
        System.out.println("\n--- RECUPERANDO ARCHIVO ---");
        
        // 1. Cargamos el IV que guardó el programa encrypt
        byte[] ivBytes = Files.readAllBytes(Paths.get("c1.iv"));
        GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);

        // 2. Cargamos la clave privada del usuario
        PrivateKey clavePrivadaRecuperada = loadPrivateKeyEncrypted(passwordUsuario, "privada.enc");
        
        // 3. Cargamos la clave AES envuelta y la desenvolvemos
        byte[] claveAESProtegida = Files.readAllBytes(Paths.get("c1.key"));
        Cipher unwrapCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        unwrapCipher.init(Cipher.UNWRAP_MODE, clavePrivadaRecuperada);
        SecretKey claveAESRecuperada = (SecretKey) unwrapCipher.unwrap(claveAESProtegida, "AES", Cipher.SECRET_KEY);
        
        System.out.println("Clave AES desenvuelta con éxito.");

        // 4. Desciframos el archivo multimedia
        System.out.println("Descifrando archivo... Generando -> " + archivoDescifrado.getName());
        decryptFile(algorithm, archivoCifrado, archivoDescifrado, claveAESRecuperada, iv);

        System.out.println("¡Proceso completado con éxito!");
    }

    public static void decryptFile(String algorithm, File inputFile, File outputFile, SecretKey key, GCMParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
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

    public static PrivateKey loadPrivateKeyEncrypted(String password, String filename) throws Exception {
        byte[] encryptedPrivateKey = Files.readAllBytes(Paths.get(filename));
        SecretKey aesKey = getAESKeyFromPassword(password);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decryptedPrivateKeyBytes = cipher.doFinal(encryptedPrivateKey);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decryptedPrivateKeyBytes));
    }

    public static SecretKey getAESKeyFromPassword(String password) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes("UTF-8"));
        return new SecretKeySpec(Arrays.copyOf(hash, 16), "AES");
    }
}