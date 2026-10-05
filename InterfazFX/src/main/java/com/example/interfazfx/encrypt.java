package com.example.interfazfx;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Arrays;

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
        
        // 1. CIFRADO DEL ARCHIVO MULTIMEDIA
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey key = keyGenerator.generateKey();

        byte[] ivBytes = new byte[12];
        new SecureRandom().nextBytes(ivBytes);
        GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);

        File archivoCifrado = new File("c1.enc");
        System.out.println("1. Cifrando " + archivoOriginal.getName() + " -> " + archivoCifrado.getName());
        encryptFile(algorithm, archivoOriginal, archivoCifrado, key, iv);

        // 2. GESTIÓN DE USUARIO (COMPROBAR SI ES LA PRIMERA VEZ)
        File archivoPublica = new File("publica.key");
        File archivoPrivada = new File("privada.enc");

        if (!archivoPublica.exists() || !archivoPrivada.exists()) {
            System.out.println("\n--- REGISTRO DE USUARIO (Primera vez) ---");
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048); 
            KeyPair parDeClaves = keyPairGen.generateKeyPair();
            
            savePublicKey(parDeClaves.getPublic(), archivoPublica.getName());
            savePrivateKeyEncrypted(parDeClaves.getPrivate(), passwordUsuario, archivoPrivada.getName());
            System.out.println("Claves RSA creadas y guardadas.");
        }

        // 3. PROTECCIÓN DE LA CLAVE Y EL IV DEL ARCHIVO
        System.out.println("\n--- PROTEGIENDO CLAVE AES E IV ---");
        PublicKey clavePublicaCargada = loadPublicKey("publica.key");
        
        Cipher wrapCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        wrapCipher.init(Cipher.WRAP_MODE, clavePublicaCargada); 
        byte[] claveAESProtegida = wrapCipher.wrap(key);
        
        // Guardamos la clave AES envuelta
        try (FileOutputStream fos = new FileOutputStream("c1.key")) {
            fos.write(claveAESProtegida);
        }
        // Guardamos los 12 bytes del IV
        try (FileOutputStream fos = new FileOutputStream("c1.iv")) {
            fos.write(ivBytes);
        }
        
        System.out.println("Clave guardada en c1.key y Vector guardado en c1.iv");
        System.out.println("¡Cifrado finalizado! Ya puedes usar el programa decrypt.");
    }

    public static void encryptFile(String algorithm, File inputFile, File outputFile, SecretKey key, GCMParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
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

    public static void savePublicKey(PublicKey publicKey, String filename) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(publicKey.getEncoded());
        }
    }

    public static PublicKey loadPublicKey(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    public static void savePrivateKeyEncrypted(PrivateKey privateKey, String password, String filename) throws Exception {
        SecretKey aesKey = getAESKeyFromPassword(password);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedPrivateKey = cipher.doFinal(privateKey.getEncoded());
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(encryptedPrivateKey);
        }
    }

    public static SecretKey getAESKeyFromPassword(String password) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes("UTF-8"));
        return new SecretKeySpec(Arrays.copyOf(hash, 16), "AES");
    }
}