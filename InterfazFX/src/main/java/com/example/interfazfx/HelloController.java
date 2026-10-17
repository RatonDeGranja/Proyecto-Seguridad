package com.example.interfazfx;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.File;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
    @FXML
    protected void onEncrypt() {
        try{
            File archivo = new File("C:\\Users\\Diable\\Desktop\\Uni\\CyS\\Proyecto-Seguridad\\InterfazFX\\Casa5.jpg");
            String password = "pallico";
            encrypt.encryptFlow(archivo, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
