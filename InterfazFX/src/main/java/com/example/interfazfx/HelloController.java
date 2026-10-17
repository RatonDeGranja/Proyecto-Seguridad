package com.example.interfazfx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class HelloController {
    @FXML private Label welcomeText;
    @FXML private TextField passwordField; // Para recoger la contraseña de la interfaz

    private File archivoSeleccionado; // Guarda el archivo que elija el usuario

    @FXML
    protected void onSeleccionarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona un archivo");
        archivoSeleccionado = fileChooser.showOpenDialog(new Stage());
        if (archivoSeleccionado != null) {
            welcomeText.setText("Archivo cargado: " + archivoSeleccionado.getName());
        }
    }

    @FXML
    protected void onEncrypt() {
        if (archivoSeleccionado == null || passwordField.getText().isEmpty()) {
            welcomeText.setText("Error: Selecciona un archivo y escribe la contraseña.");
            return;
        }
        try {
            welcomeText.setText("Cifrando y conectando a MySQL...");
            encrypt.encryptFlow(archivoSeleccionado, passwordField.getText());
            welcomeText.setText("¡Cifrado completado con éxito!");
        } catch (Exception e) {
            welcomeText.setText("Error crítico: " + e.getMessage());
        }
    }

    @FXML
    protected void onDecrypt() {
        if (archivoSeleccionado == null || passwordField.getText().isEmpty()) {
            welcomeText.setText("Error: Selecciona el archivo .enc y escribe la contraseña.");
            return;
        }
        try {
            welcomeText.setText("Descifrando desde MySQL...");
            // Autogenera un nombre para el archivo recuperado
            File carpeta = new File(System.getProperty("user.home"), "CarpetaSegura");
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }

            String nombreLimpio = archivoSeleccionado.getName().replace(".enc", "");
            File archivoDescifrado = new File(carpeta, "recuperado_" + nombreLimpio);

            decrypt.decryptFlow(archivoSeleccionado, passwordField.getText(), archivoDescifrado);
            welcomeText.setText("¡Descifrado con éxito! Guardado como: " + archivoDescifrado.getName());
        } catch (Exception e) {
            welcomeText.setText("Error al descifrar. ¿Es correcta la contraseña?");
            welcomeText.setText(e.getMessage());
        }
    }
}