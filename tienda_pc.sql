-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: 127.0.0.1    Database: tienda_pc
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `tienda_pc`
--

/*!40000 DROP DATABASE IF EXISTS `tienda_pc`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `tienda_pc` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `tienda_pc`;

--
-- Table structure for table `categoria`
--

DROP TABLE IF EXISTS `categoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `categoria` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK35t4wyxqrevf09uwx9e9p6o75` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categoria`
--

LOCK TABLES `categoria` WRITE;
/*!40000 ALTER TABLE `categoria` DISABLE KEYS */;
INSERT INTO `categoria` VALUES (5,'Almacenamiento'),(8,'Audio'),(10,'Consolas'),(4,'Memorias RAM'),(6,'Monitores'),(7,'Perifericos'),(3,'Placas Madre'),(2,'Procesadores'),(9,'Sillas Gamer'),(1,'Tarjetas Graficas');
/*!40000 ALTER TABLE `categoria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cliente`
--

DROP TABLE IF EXISTS `cliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cliente` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `apellidos` varchar(100) NOT NULL,
  `direccion` varchar(200) DEFAULT NULL,
  `dni` varchar(8) NOT NULL,
  `email` varchar(120) DEFAULT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `nombres` varchar(100) NOT NULL,
  `telefono` varchar(15) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjlcg5nhnauli1hu4ojldsedaw` (`dni`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cliente`
--

LOCK TABLES `cliente` WRITE;
/*!40000 ALTER TABLE `cliente` DISABLE KEYS */;
INSERT INTO `cliente` VALUES (1,'DE LA SOTA CASTRO','Av. Arequipa 1234, Lima','70123456','carlos.quispe@gmail.com','2026-07-16 01:39:19.000000','FIORELLA','987654321'),(2,'PAUCAR TTUPA','Jr. Cusco 567, San Isidro','72345678','maria.rojas@hotmail.com','2026-07-16 01:39:19.000000','JAZMIN','912345678'),(3,'CALACHUA CONDORCAHUANA','Calle Las Begonias 89, Miraflores','75987654','diego.fdz@outlook.com','2026-07-16 01:39:19.000000','WILIAM','956123789'),(4,'ROJAS MORE','Av. Brasil 2345, Jesus Maria','76543210','lucia.mendoza@gmail.com','2026-07-16 01:39:19.000000','CARLOS EDUARDO','999888777'),(5,'SANCHEZ CUEVA','Av. La Marina 456, San Miguel','78901234','andres.castillo@gmail.com','2026-07-16 01:39:20.000000','YULISA VALERIA','987111222'),(6,'PALACIOS CUZCANO','Av. Javier Prado 789, San Borja','71222333','valeria.torres@gmail.com','2026-07-16 01:39:20.000000','MICHAEL ROBBIANO','955444333');
/*!40000 ALTER TABLE `cliente` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pedido`
--

DROP TABLE IF EXISTS `pedido`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pedido` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `estado` varchar(30) NOT NULL,
  `fecha` datetime(6) NOT NULL,
  `metodo_pago` varchar(30) DEFAULT NULL,
  `total` double NOT NULL,
  `cliente_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK30s8j2ktpay6of18lbyqn3632` (`cliente_id`),
  CONSTRAINT `FK30s8j2ktpay6of18lbyqn3632` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pedido`
--

LOCK TABLES `pedido` WRITE;
/*!40000 ALTER TABLE `pedido` DISABLE KEYS */;
INSERT INTO `pedido` VALUES (1,'PAGADO','2026-02-23 17:33:20.000000','TRANSFERENCIA',5198,6),(2,'ENVIADO','2026-02-13 11:09:20.000000','EFECTIVO',5696,4),(3,'ENVIADO','2026-05-29 18:31:20.000000','TARJETA',19995,6),(4,'ENVIADO','2026-04-25 22:23:20.000000','YAPE',1899,4),(5,'CANCELADO','2026-07-14 08:07:20.000000','YAPE',4904,3),(6,'PAGADO','2026-03-22 06:46:20.000000','YAPE',3048,6),(7,'ENTREGADO','2026-07-10 01:56:20.000000','YAPE',15446,4),(8,'PAGADO','2026-01-31 08:57:20.000000','TARJETA',3243,1),(9,'ENVIADO','2026-06-02 16:02:20.000000','TRANSFERENCIA',16844,1),(10,'PENDIENTE','2026-06-07 09:32:20.000000','YAPE',5693,6),(11,'PENDIENTE','2026-04-27 14:02:20.000000','TRANSFERENCIA',4596,5),(12,'PAGADO','2026-03-04 22:04:20.000000','PLIN',3996,2),(13,'ENTREGADO','2026-06-21 04:43:20.000000','TRANSFERENCIA',1699,6),(14,'PENDIENTE','2026-07-10 16:54:20.000000','EFECTIVO',1198,1),(15,'ENTREGADO','2026-06-29 06:10:20.000000','TRANSFERENCIA',3704,6),(16,'PENDIENTE','2026-02-15 21:40:20.000000','TARJETA',6244,1),(17,'CANCELADO','2026-05-03 11:08:20.000000','TRANSFERENCIA',9193,3),(18,'PAGADO','2026-01-25 09:10:20.000000','EFECTIVO',2445,4),(19,'ENTREGADO','2026-04-08 06:57:20.000000','EFECTIVO',5697,3),(20,'ENVIADO','2026-06-02 21:32:20.000000','TARJETA',15341,5),(21,'ENTREGADO','2026-06-25 17:43:20.000000','TRANSFERENCIA',7196,2),(22,'PAGADO','2026-02-24 19:17:20.000000','TRANSFERENCIA',1798,3),(23,'ENTREGADO','2026-05-28 18:12:20.000000','EFECTIVO',3445,6),(24,'ENTREGADO','2026-03-17 00:44:20.000000','EFECTIVO',3897,3),(25,'ENVIADO','2026-03-14 09:40:20.000000','EFECTIVO',3545,2),(26,'PAGADO','2026-05-02 12:11:20.000000','PLIN',4097,5),(27,'ENVIADO','2026-07-03 04:15:20.000000','PLIN',2297,4),(28,'ENVIADO','2026-05-24 18:28:20.000000','TARJETA',6897,5),(29,'PAGADO','2026-05-24 05:22:20.000000','YAPE',19690,4),(30,'CANCELADO','2026-05-29 07:48:20.000000','PLIN',6875,3),(31,'ENTREGADO','2026-04-09 08:22:20.000000','TRANSFERENCIA',11492,4),(32,'PENDIENTE','2026-03-12 23:42:20.000000','TRANSFERENCIA',949,4),(33,'CANCELADO','2026-04-16 23:14:20.000000','YAPE',4497,3),(34,'ENTREGADO','2026-04-30 02:06:20.000000','TARJETA',9998,6),(35,'ENTREGADO','2026-03-17 17:41:20.000000','EFECTIVO',9992,5),(36,'ENTREGADO','2026-02-13 23:45:20.000000','YAPE',549,4),(37,'PENDIENTE','2026-03-04 14:08:20.000000','TRANSFERENCIA',8443,1),(38,'ENVIADO','2026-04-14 09:59:20.000000','YAPE',2648,5),(39,'PENDIENTE','2026-05-04 13:10:20.000000','YAPE',9052,2),(40,'ENTREGADO','2026-02-23 08:38:20.000000','PLIN',12793,4);
/*!40000 ALTER TABLE `pedido` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pedido_item`
--

DROP TABLE IF EXISTS `pedido_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pedido_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cantidad` int(11) NOT NULL,
  `precio_unitario` double NOT NULL,
  `pedido_id` bigint(20) NOT NULL,
  `producto_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKeyouxfvoi291lpo5168e6wpej` (`pedido_id`),
  KEY `FKjlwj85ummnhb4hegddffa85pe` (`producto_id`),
  CONSTRAINT `FKeyouxfvoi291lpo5168e6wpej` FOREIGN KEY (`pedido_id`) REFERENCES `pedido` (`id`),
  CONSTRAINT `FKjlwj85ummnhb4hegddffa85pe` FOREIGN KEY (`producto_id`) REFERENCES `producto` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=95 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pedido_item`
--

LOCK TABLES `pedido_item` WRITE;
/*!40000 ALTER TABLE `pedido_item` DISABLE KEYS */;
INSERT INTO `pedido_item` VALUES (1,2,2599,1,2),(2,1,1899,2,17),(3,2,949,2,10),(4,1,1899,2,6),(5,3,4999,3,3),(6,2,2499,3,26),(7,1,1899,4,17),(8,1,2199,5,23),(9,2,329,5,14),(10,1,449,5,25),(11,2,799,5,15),(12,1,449,6,25),(13,1,2599,6,2),(14,1,449,7,25),(15,3,4999,7,3),(16,1,699,8,9),(17,3,599,8,13),(18,3,249,8,12),(19,3,4999,9,3),(20,1,1149,9,11),(21,2,349,9,22),(22,2,899,10,24),(23,3,349,10,22),(24,1,1699,10,8),(25,1,1149,10,11),(26,3,699,11,9),(27,1,2499,11,26),(28,3,699,12,9),(29,1,1899,12,6),(30,1,1699,13,8),(31,2,599,14,21),(32,2,329,15,14),(33,1,949,15,10),(34,3,699,15,9),(35,3,1149,16,11),(36,1,899,16,24),(37,2,949,16,10),(38,3,1399,17,1),(39,3,899,17,24),(40,1,2299,17,4),(41,1,599,18,21),(42,1,499,18,20),(43,3,449,18,25),(44,3,1899,19,6),(45,1,549,20,5),(46,3,2599,20,2),(47,3,1699,20,8),(48,2,949,20,10),(49,1,2699,21,27),(50,3,1499,21,28),(51,2,899,22,19),(52,3,349,23,22),(53,2,1199,23,7),(54,1,499,24,20),(55,2,1699,24,8),(56,3,899,25,24),(57,1,249,25,12),(58,1,599,25,13),(59,2,1199,26,7),(60,1,1699,26,8),(61,2,549,27,5),(62,1,1199,27,7),(63,3,2299,28,18),(64,3,2599,29,2),(65,2,1199,29,7),(66,3,1899,29,6),(67,2,1899,29,17),(68,1,449,30,25),(69,2,549,30,16),(70,1,329,30,14),(71,1,4999,30,3),(72,3,2499,31,26),(73,2,1149,31,11),(74,1,699,31,9),(75,2,499,31,20),(76,1,949,32,10),(77,3,1499,33,28),(78,2,4999,34,3),(79,2,799,35,15),(80,3,2299,35,18),(81,3,499,35,20),(82,1,549,36,5),(83,2,2699,37,27),(84,2,1149,37,11),(85,3,249,37,12),(86,1,2299,38,4),(87,1,349,38,22),(88,2,2199,39,23),(89,2,1499,39,28),(90,2,329,39,14),(91,2,499,39,20),(92,2,249,40,12),(93,3,2299,40,4),(94,2,2699,40,27);
/*!40000 ALTER TABLE `pedido_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `producto`
--

DROP TABLE IF EXISTS `producto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `producto` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(500) DEFAULT NULL,
  `imagen` varchar(500) DEFAULT NULL,
  `nombre` varchar(255) NOT NULL,
  `precio` double NOT NULL,
  `stock` int(11) NOT NULL,
  `categoria_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9su14n91mtgcg5ehl658v4afx` (`nombre`),
  KEY `FKodqr7965ok9rwquj1utiamt0m` (`categoria_id`),
  CONSTRAINT `FKodqr7965ok9rwquj1utiamt0m` FOREIGN KEY (`categoria_id`) REFERENCES `categoria` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `producto`
--

LOCK TABLES `producto` WRITE;
/*!40000 ALTER TABLE `producto` DISABLE KEYS */;
INSERT INTO `producto` VALUES (1,'Tarjeta grafica 8GB GDDR6 DLSS 3 1080p/1440p','/images/productos/rtx-4060.jpg','NVIDIA GeForce RTX 4060',1399,14,1),(2,'Tarjeta grafica 12GB GDDR6X ideal 1440p','/images/productos/rtx-4070.jpg','NVIDIA GeForce RTX 4070',2599,9,1),(3,'Tarjeta grafica 16GB GDDR6X 4K high-end','/images/productos/rtx-4080.jpg','NVIDIA GeForce RTX 4080 Super',4999,5,1),(4,'Tarjeta grafica 16GB GDDR6 1440p rasterizado','/images/productos/rx-7800xt.jpg','AMD Radeon RX 7800 XT',2299,7,1),(5,'Procesador 6 nucleos 12 hilos AM4 3.5GHz','/images/productos/ryzen-5-5600.jpg','AMD Ryzen 5 5600',549,20,2),(6,'Procesador gaming 8 nucleos 3D V-Cache AM5','/images/productos/ryzen-7-7800x3d.jpg','AMD Ryzen 7 7800X3D',1899,11,2),(7,'Procesador 14 nucleos LGA1700 hasta 5.1GHz','/images/productos/intel-i5-13600k.jpg','Intel Core i5-13600K',1199,13,2),(8,'Procesador 16 nucleos LGA1700 hasta 5.4GHz','/images/productos/intel-i7-13700k.jpg','Intel Core i7-13700K',1699,8,2),(9,'Placa madre AM4 DDR4 ATX PCIe 4.0','/images/productos/mb-b550.jpg','ASUS TUF Gaming B550-PLUS',699,15,3),(10,'Placa madre AM5 DDR5 ATX WiFi','/images/productos/mb-b650.jpg','MSI MAG B650 Tomahawk',949,10,3),(11,'Placa madre LGA1700 DDR5 ATX','/images/productos/mb-z790.jpg','Gigabyte Z790 AORUS Elite',1149,6,3),(12,'Memoria RAM 2x8GB 3200MHz CL16','/images/productos/ram-vengeance-16.jpg','Corsair Vengeance 16GB DDR4',249,30,4),(13,'Memoria RAM 2x16GB 6000MHz RGB','/images/productos/ram-vengeance-32.jpg','Corsair Vengeance 32GB DDR5',599,18,4),(14,'SSD M.2 PCIe 3.0 hasta 3500MB/s','/images/productos/ssd-980-1tb.jpg','Samsung 980 NVMe 1TB',329,25,5),(15,'SSD M.2 PCIe 4.0 gaming hasta 7300MB/s','/images/productos/ssd-sn850x-2tb.jpg','WD Black SN850X 2TB',799,12,5),(16,'Fuente de poder 750W modular certificada','/images/productos/psu-rm750.jpg','Corsair RM750 80+ Gold',549,16,3),(17,'Monitor curvo QHD 240Hz 1ms','/images/productos/monitor-odyssey-g7.jpg','Samsung Odyssey G7 27\"',1899,8,6),(18,'Monitor ultrawide 160Hz Nano IPS','/images/productos/monitor-lg-ultragear.jpg','LG UltraGear 34\" UWQHD',2299,6,6),(19,'Teclado mecanico RGB switches verdes','/images/productos/teclado-blackwidow.jpg','Razer BlackWidow V4 Pro',899,22,7),(20,'Mouse inalambrico 63g sensor HERO 25K','/images/productos/mouse-gpro-superlight.jpg','Logitech G Pro X Superlight',499,28,7),(21,'Webcam 4K UHD para streaming','/images/productos/webcam-brio.jpg','Logitech Brio 4K',599,14,7),(22,'Auriculares gaming 7.1 con microfono','/images/productos/headset-cloud-iii.jpg','HyperX Cloud III',349,26,8),(23,'Silla gaming ergonomica cuero NEO talla R','/images/productos/silla-titan-evo.jpg','Secretlab Titan Evo',2199,7,9),(24,'Silla gamer reclinable con cojines','/images/productos/silla-cougar.jpg','Cougar Armor One',899,12,9),(25,'Refrigeracion liquida 240mm ARGB','/images/productos/cooler-aio.jpg','Cooler Master ML240L AIO',449,15,3),(26,'Consola Sony PS5 Slim 1TB edicion digital','/images/productos/ps5-slim.jpg','PlayStation 5 Slim',2499,10,10),(27,'Consola Microsoft 1TB 4K 120Hz','/images/productos/xbox-series-x.jpg','Xbox Series X',2699,8,10),(28,'Consola hibrida pantalla OLED 7\"','/images/productos/switch-oled.jpg','Nintendo Switch OLED',1499,16,10);
/*!40000 ALTER TABLE `producto` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refresh_token`
--

DROP TABLE IF EXISTS `refresh_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `refresh_token` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `expira_en` datetime(6) NOT NULL,
  `revocado` bit(1) NOT NULL,
  `token` varchar(100) NOT NULL,
  `usuario_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr4k4edos30bx9neoq81mdvwph` (`token`),
  KEY `FKpr5fysa08n2o6i1rxjumfl89q` (`usuario_id`),
  CONSTRAINT `FKpr5fysa08n2o6i1rxjumfl89q` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refresh_token`
--

LOCK TABLES `refresh_token` WRITE;
/*!40000 ALTER TABLE `refresh_token` DISABLE KEYS */;
INSERT INTO `refresh_token` VALUES (1,'2026-07-23 01:39:59.000000','','fa0467ff68314e0ca2c265f9e2313cb2',1),(2,'2026-07-23 01:40:01.000000','\0','ccb24821bee546e88a1dcd77e3aa9144',1),(3,'2026-07-23 01:46:45.000000','\0','555fcab125e047aab824d2afa81be544',1),(4,'2026-07-23 01:49:59.000000','\0','bfc8adf63bef457baedb7f0b4faafdd4',1),(5,'2026-07-23 01:51:31.000000','\0','7eac3557eda2485fbd9b922116c727dd',1),(6,'2026-07-23 01:52:01.000000','\0','15f368b9796c4601835fa7aff40d63cb',1),(7,'2026-07-23 02:01:02.000000','\0','5b6f8d42d17446049e2adf51912ac4df',1),(8,'2026-07-23 02:38:27.000000','','13610393d4c54b208c45793191e42a50',1),(9,'2026-07-23 02:43:29.000000','','47c2578c2d174b88b4f4833917d5cd44',1),(10,'2026-07-23 02:57:53.000000','','e16809d7e32e43059c7c9911d765cc56',1),(11,'2026-07-23 03:09:07.000000','','92ba90ba52844daa8d2c6746ec0081b7',1),(12,'2026-07-23 03:14:17.000000','','80428aeca7b14004a9be1d389e45fd86',1),(13,'2026-07-23 05:11:56.000000','\0','2af527edd59c40c390f2747bc2d0776e',1);
/*!40000 ALTER TABLE `refresh_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usuario` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(120) NOT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `password` varchar(100) NOT NULL,
  `rol` enum('ADMIN','USUARIO') NOT NULL,
  `telefono` varchar(15) DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5171l57faosmj8myawaucatdw` (`email`),
  UNIQUE KEY `UK863n1y3x0jalatoir4325ehal` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES (1,'admin123@gamerstore.com','2026-07-16 01:39:20.000000','Administrador','$2a$10$/RiGAkVtkLOXJsAAdFrfkOjXRkCAwd5e5kzhjbmP9VysZ/iFLfSuq','ADMIN','986969024','admin123');
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'tienda_pc'
--

--
-- Dumping routines for database 'tienda_pc'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-16 10:51:58
