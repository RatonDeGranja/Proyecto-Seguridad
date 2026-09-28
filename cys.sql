-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 28-09-2026 a las 09:54:22
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `cys`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `archivos_multimedia`
--

CREATE TABLE `archivos_multimedia` (
  `id_archivo` int(11) NOT NULL,
  `nombre_original` varchar(255) NOT NULL,
  `ruta_cifrado` varchar(255) NOT NULL,
  `clave_aes_cifrada` blob NOT NULL,
  `vector_inicializacion_iv` blob NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `credenciales_rsa`
--

CREATE TABLE `credenciales_rsa` (
  `id` int(11) NOT NULL,
  `clave_publica_rsa` blob NOT NULL,
  `clave_privada_cifrada` blob NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `archivos_multimedia`
--
ALTER TABLE `archivos_multimedia`
  ADD PRIMARY KEY (`id_archivo`);

--
-- Indices de la tabla `credenciales_rsa`
--
ALTER TABLE `credenciales_rsa`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `archivos_multimedia`
--
ALTER TABLE `archivos_multimedia`
  MODIFY `id_archivo` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `credenciales_rsa`
--
ALTER TABLE `credenciales_rsa`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
