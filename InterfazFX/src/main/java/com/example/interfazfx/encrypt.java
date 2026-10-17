package com.example.interfazfx;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Arrays;
import java.sql.*;

public class encrypt {
    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Error: Faltan argumentos.");
            System.out.println("Ejecuta asi: java encrypt mivideo.mp4 MiContrasena");
            return;
        }

        try {
            encryptFlow(new File(args[0]), args[1]);
        } catch (Exception e) {
            System.err.println("Ha ocurrido un error criptográfico:");
            e.printStackTrace();
        }
    }

    static void encryptFlow(File archivoOriginal, String passwordUsuario) throws Exception {
        String algorithm = "AES/GCM/NoPadding";
        
        // Crear clave aleatoria con AES128
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey key = keyGenerator.generateKey();

        byte[] ivBytes = new byte[12];
        new SecureRandom().nextBytes(ivBytes);
        GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);

        File carpeta = new File(System.getProperty("user.home"), "CarpetaSegura");
        if (!carpeta.exists()) {
            carpeta.mkdirs(); // Crea la carpeta si es la primera vez
        }

        File archivoCifrado = new File(carpeta, archivoOriginal.getName() + ".enc");
        System.out.println("Cifrando " + archivoOriginal.getName() + " -> " + archivoCifrado.getName());
        
        encryptFile(algorithm, archivoOriginal, archivoCifrado, key, iv, ivBytes);

        //GESTIÓN DE USUARIO (COMPROBAR SI ES LA PRIMERA VEZ)
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM credenciales_rsa")) {
            
            rs.next();
            if (rs.getInt("total") == 0) {
                System.out.println("\n--- REGISTRO DE USUARIO EN BD (Primera vez) ---");
                KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
                keyPairGen.initialize(2048); 
                KeyPair parDeClaves = keyPairGen.generateKeyPair();
                
                byte[] ivPrivada = new byte[12];
                new SecureRandom().nextBytes(ivPrivada);
                GCMParameterSpec specPrivada = new GCMParameterSpec(128, ivPrivada);

                SecretKey aesKey = getAESKeyFromPassword(passwordUsuario);
                Cipher cipherPriv = Cipher.getInstance("AES/GCM/NoPadding");
                cipherPriv.init(Cipher.ENCRYPT_MODE, aesKey, specPrivada);
                byte[] encryptedPrivateKey = cipherPriv.doFinal(parDeClaves.getPrivate().getEncoded());

                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(12 + encryptedPrivateKey.length);
                buffer.put(ivPrivada);
                buffer.put(encryptedPrivateKey);
                byte[] paquetePrivadaFinal = buffer.array();

                String sqlRsa = "INSERT INTO credenciales_rsa (clave_publica_rsa, clave_privada_cifrada) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlRsa)) {
                    pstmt.setBytes(1, parDeClaves.getPublic().getEncoded());
                    pstmt.setBytes(2, paquetePrivadaFinal);
                    pstmt.executeUpdate();
                }
                System.out.println("Claves RSA guardadas en MySQL");
            }
        }

        System.out.println("\n--- PROTEGIENDO Y GUARDANDO EN MYSQL ---");
        PublicKey clavePublicaCargada = null;
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT clave_publica_rsa FROM credenciales_rsa ORDER BY id DESC LIMIT 1")) {
            if (rs.next()) {
                byte[] keyBytes = rs.getBytes("clave_publica_rsa");
                clavePublicaCargada = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
            }
        }

        Cipher wrapCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        wrapCipher.init(Cipher.WRAP_MODE, clavePublicaCargada); 
        byte[] claveAESProtegida = wrapCipher.wrap(key);
        
        String sqlArchivo = "INSERT INTO archivos_multimedia (nombre_original, ruta_cifrado, clave_aes_cifrada) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sqlArchivo)) {
            pstmt.setString(1, archivoOriginal.getName());
            pstmt.setString(2, archivoCifrado.getAbsolutePath());
            pstmt.setBytes(3, claveAESProtegida);
            pstmt.executeUpdate();
        }
        System.out.println("Criptograma registrado en archivos_multimedia.");
        System.out.println("¡Cifrado finalizado! Ya puedes usar el programa decrypt.");
        
    }

    public static void encryptFile(String algorithm, File inputFile, File outputFile, SecretKey key, GCMParameterSpec iv, byte[] ivBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
             
            // Se escribe el iv al principio del archivo
            outputStream.write(ivBytes); 

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