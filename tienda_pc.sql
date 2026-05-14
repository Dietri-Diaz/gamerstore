-- =====================================================================
-- GamerStore — dump limpio para tienda_pc (MariaDB / XAMPP / phpMyAdmin)
-- Esquema del Avance 2:
--   - usuario   (solo admin del ERP, password con BCrypt)
--   - cliente   (compradores, sin login, con DNI unico)
--   - categoria (tags de productos)
--   - producto  (catalogo con FK a categoria)
--   - pedido    (ventas, FK a cliente — se llenara en Avance 3 via POS)
--   - pedido_item (detalle de cada venta, FK a pedido y producto)
--
-- Nota: Spring Boot + Hibernate (`ddl-auto=update`) crea/actualiza las tablas
-- al arranque. Este archivo es para ejecucion manual en phpMyAdmin si se
-- desea inicializar la BD desde cero sin arrancar la app.
-- =====================================================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `tienda_pc`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `tienda_pc`;

-- ---------------------------------------------------------------------
-- Limpieza
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `pedido_item`;
DROP TABLE IF EXISTS `pedido`;
DROP TABLE IF EXISTS `carrito_item`;
DROP TABLE IF EXISTS `carrito`;
DROP TABLE IF EXISTS `producto`;
DROP TABLE IF EXISTS `categoria`;
DROP TABLE IF EXISTS `cliente`;
DROP TABLE IF EXISTS `usuario`;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- USUARIO (empleados del ERP)
-- ---------------------------------------------------------------------
CREATE TABLE `usuario` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(120) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `password` varchar(100) NOT NULL COMMENT 'Hash BCrypt',
  `telefono` varchar(15) DEFAULT NULL,
  `fecha_registro` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `rol` varchar(20) NOT NULL DEFAULT 'ADMIN',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usuario_username` (`username`),
  UNIQUE KEY `uk_usuario_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- CLIENTE (compradores de la tienda — sin login)
-- ---------------------------------------------------------------------
CREATE TABLE `cliente` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dni` varchar(8) NOT NULL,
  `nombres` varchar(100) NOT NULL,
  `apellidos` varchar(100) NOT NULL,
  `telefono` varchar(15) DEFAULT NULL,
  `email` varchar(120) DEFAULT NULL,
  `direccion` varchar(200) DEFAULT NULL,
  `fecha_registro` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cliente_dni` (`dni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- CATEGORIA
-- ---------------------------------------------------------------------
CREATE TABLE `categoria` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_categoria_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- PRODUCTO
-- ---------------------------------------------------------------------
CREATE TABLE `producto` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(200) NOT NULL,
  `descripcion` varchar(500) DEFAULT NULL,
  `precio` double NOT NULL,
  `imagen` varchar(500) DEFAULT NULL,
  `stock` int(11) NOT NULL DEFAULT 0,
  `categoria_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_producto_categoria` (`categoria_id`),
  CONSTRAINT `fk_producto_categoria` FOREIGN KEY (`categoria_id`)
      REFERENCES `categoria` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- PEDIDO (se usara en el Avance 3 con el modulo POS)
-- ---------------------------------------------------------------------
CREATE TABLE `pedido` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cliente_id` bigint(20) NOT NULL,
  `fecha` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` varchar(30) NOT NULL DEFAULT 'PENDIENTE',
  `total` double NOT NULL,
  `metodo_pago` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pedido_cliente` (`cliente_id`),
  CONSTRAINT `fk_pedido_cliente` FOREIGN KEY (`cliente_id`)
      REFERENCES `cliente` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- PEDIDO_ITEM
-- ---------------------------------------------------------------------
CREATE TABLE `pedido_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pedido_id` bigint(20) NOT NULL,
  `producto_id` bigint(20) NOT NULL,
  `cantidad` int(11) NOT NULL,
  `precio_unitario` double NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pedidoitem_pedido` (`pedido_id`),
  KEY `fk_pedidoitem_producto` (`producto_id`),
  CONSTRAINT `fk_pedidoitem_pedido` FOREIGN KEY (`pedido_id`)
      REFERENCES `pedido` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pedidoitem_producto` FOREIGN KEY (`producto_id`)
      REFERENCES `producto` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- DATOS INICIALES
-- (DataSeeder.java tambien los crea si no existen, este SQL es opcional)
-- =====================================================================

-- Categorias
INSERT INTO `categoria` (`id`, `nombre`) VALUES
  (1, 'Consolas'),
  (2, 'Perifericos'),
  (3, 'Monitores'),
  (4, 'Sillas'),
  (5, 'Streaming'),
  (6, 'Mandos'),
  (7, 'VR');

-- Productos
INSERT INTO `producto` (`nombre`, `descripcion`, `precio`, `imagen`, `stock`, `categoria_id`) VALUES
  ('PlayStation 5 Slim', 'Consola Sony PS5 Slim 1TB edicion digital', 2499.00, 'https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=600', 15, 1),
  ('Xbox Series X', 'Consola Microsoft Xbox Series X 1TB 4K 120Hz', 2699.00, 'https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=600', 10, 1),
  ('Nintendo Switch OLED', 'Consola Nintendo Switch modelo OLED blanca', 1499.00, 'https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?w=600', 20, 1),
  ('Logitech G Pro X Superlight', 'Mouse gaming inalambrico 63g sensor HERO 25K', 499.00, 'https://images.unsplash.com/photo-1527814050087-3793815479db?w=600', 30, 2),
  ('Razer BlackWidow V4 Pro', 'Teclado mecanico RGB switches verdes Chroma', 899.00, 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=600', 25, 2),
  ('HyperX Cloud III', 'Auriculares gaming 7.1 DTS con microfono', 349.00, 'https://images.unsplash.com/photo-1599669454699-248893623440?w=600', 40, 2),
  ('Samsung Odyssey G7 27"', 'Monitor gaming curvo QHD 240Hz 1ms', 1899.00, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600', 8, 3),
  ('Secretlab Titan Evo', 'Silla gaming ergonomica talla R cuero NEO', 2199.00, 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?w=600', 8, 4),
  ('Elgato Stream Deck MK.2', 'Controlador para streaming 15 teclas LCD', 599.00, 'https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=600', 18, 5),
  ('DualSense Edge PS5', 'Mando PS5 profesional personalizable', 799.00, 'https://images.unsplash.com/photo-1592840496694-26d035b52b48?w=600', 22, 6),
  ('Meta Quest 3', 'Visor realidad virtual 128GB wireless', 1799.00, 'https://images.unsplash.com/photo-1622979135225-d2ba269cf1ac?w=600', 9, 7),
  ('LG UltraGear 34"', 'Monitor ultrawide QHD 160Hz Nano IPS', 2299.00, 'https://images.unsplash.com/photo-1616711906333-23cf8b918a76?w=600', 6, 3);

-- Clientes demo
INSERT INTO `cliente` (`dni`, `nombres`, `apellidos`, `telefono`, `email`, `direccion`) VALUES
  ('70123456', 'Carlos', 'Quispe Vargas', '987654321', 'carlos.quispe@gmail.com', 'Av. Arequipa 1234, Lima'),
  ('72345678', 'Maria', 'Rojas Gomez', '912345678', 'maria.rojas@hotmail.com', 'Jr. Cusco 567, San Isidro'),
  ('75987654', 'Diego', 'Fernandez Torres', '956123789', 'diego.fdz@outlook.com', 'Calle Las Begonias 89, Miraflores'),
  ('76543210', 'Lucia', 'Mendoza Salas', '999888777', 'lucia.mendoza@gmail.com', 'Av. Brasil 2345, Jesus Maria'),
  ('78901234', 'Andres', 'Castillo Ruiz', '987111222', NULL, NULL);

-- Admin (el password hasheado lo genera el DataSeeder de Spring al arrancar)
-- Si quieres crearlo manualmente en SQL, ejecuta la app una vez y copia el hash
-- desde la tabla usuario despues de que Spring lo siembre.
