#!/bin/bash
# Bases de datos: una por microservicio (database-per-service, ADR-002).
# PostgreSQL ejecuta este script solo en el primer arranque del contenedor (volumen vacío).
# Cada servicio tiene su propio rol, dueño únicamente de su base (mínimo privilegio):
# un servicio comprometido no puede leer ni modificar las bases de los demás.
# Las contraseñas llegan por variables de entorno (archivo .env), nunca están en el repositorio.
set -euo pipefail

crear_base() {
    local rol="$1" base="$2" clave="$3"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
         --set=rol="$rol" --set=base="$base" --set=clave="$clave" <<'SQL'
CREATE ROLE :"rol" LOGIN PASSWORD :'clave';
CREATE DATABASE :"base" OWNER :"rol" ENCODING 'UTF8';
REVOKE ALL ON DATABASE :"base" FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE :"base" TO :"rol";
SQL
}

crear_base auth_svc       edusecurity_auth       "${DB_PASSWORD_AUTH:?Falta DB_PASSWORD_AUTH}"
crear_base contenido_svc  edusecurity_contenido  "${DB_PASSWORD_CONTENIDO:?Falta DB_PASSWORD_CONTENIDO}"
crear_base progreso_svc   edusecurity_progreso   "${DB_PASSWORD_PROGRESO:?Falta DB_PASSWORD_PROGRESO}"
crear_base simulacion_svc edusecurity_simulacion "${DB_PASSWORD_SIMULACION:?Falta DB_PASSWORD_SIMULACION}"
