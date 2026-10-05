# 🔒 Compresión y Seguridad (2026-2027) - Trabajo Práctico

Este repositorio contiene el desarrollo del trabajo práctico para la asignatura de **Compresión y Seguridad**. El objetivo principal es la implementación en Java de un servicio seguro de contenidos multimedia con ejecución en Windows, utilizando criptografía híbrida.

## 📌 Estado del Proyecto: Fase I en proceso.

En esta primera fase se ha desarrollado el **Módulo Criptográfico Core**, diseñado para funcionar mediante línea de comandos (CLI) de forma independiente, separando el flujo de cifrado y descifrado.

Falta hacer la interfaz gráfica e integrarla en el codigo. Además hay que hacer a base de datos.

### ✨ Características Implementadas (Criptografía)

*   **Cifrado de archivos multimedia:** Soporte para imágenes, audio y vídeo utilizando **AES-128** en modo GCM por procesamiento de flujos (Streams) para optimizar el uso de RAM con archivos pesados.
*   **Generación de Claves de Usuario:** Generación de pares de claves asimétricas **RSA-2048** para cada usuario.
*   **Protección (Wrap) de Claves AES:** La clave simétrica de cada archivo se cifra utilizando la clave pública RSA del usuario para su almacenamiento seguro.
*   **Protección de Clave Privada (PBKDF):** La clave privada RSA del usuario se protege cifrándola localmente con AES, derivando la clave de cifrado a partir del **Hash SHA-256** de una contraseña introducida por el usuario.
*   **Gestión de Vectores de Inicialización (IV):** Extracción y guardado automático de los 12 bytes del IV necesarios para descifrar el estándar AES/GCM.

## 📂 Estructura de Archivos

* `InterfazFX/src/main/java/com/example/interfazfx/encrypt.java`: Módulo encargado de registrar al usuario (si es la primera vez), generar claves, cifrar el archivo multimedia y exportar el criptograma (`.enc`), la clave envuelta (`.key`) y el vector (`.iv`).
* `InterfazFX/src/main/java/com/example/interfazfx/decrypt.java`: Módulo que autentica al usuario mediante su contraseña, recupera su clave privada, desenvuelve la clave AES del archivo y descifra el contenido multimedia devolviéndolo a su formato original.

## 🚀 Instrucciones de Uso

### 1. Compilación
Abre la terminal en la carpeta del proyecto y compila los dos módulos:
```bash
javac encrypt.java decrypt.java
```

### 2. Ejecución
Hasta que esté la interfaz gráfica y la base de datos falseada hay que ejecutar indicando el nombre y la extension del archivo a desencriptar

Para encriptar:
``` bash
java encrypt archivo.extension contraseña
```

Para desencriptar:
``` bash
java decrypt c1.enc contraseña nombreDesencriptado.extension
```