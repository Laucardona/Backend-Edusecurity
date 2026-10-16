#!/bin/bash
# Esquemas de base de datos: uno por microservicio (database-per-service).
# MySQL ejecuta este script en el primer arranque del contenedor.
# La contraseña del usuario de aplicación llega por la variable DB_PASSWORD (archivo .env).
set -euo pipefail

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS edusecurity_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edusecurity_contenido CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edusecurity_progreso CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edusecurity_simulacion CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Usuario dedicado: solo accede a los esquemas de la aplicación
CREATE USER IF NOT EXISTS 'edusecurity'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON edusecurity_auth.* TO 'edusecurity'@'%';
GRANT ALL PRIVILEGES ON edusecurity_contenido.* TO 'edusecurity'@'%';
GRANT ALL PRIVILEGES ON edusecurity_progreso.* TO 'edusecurity'@'%';
GRANT ALL PRIVILEGES ON edusecurity_simulacion.* TO 'edusecurity'@'%';
FLUSH PRIVILEGES;
SQL
