# Backend EduSecurity

Backend de la plataforma **EduSecurity** reestructurado como **arquitectura de microservicios** con Spring Boot 3.5, Spring Cloud (Gateway + Eureka + Config Server), JWT, bases de datos por servicio y despliegue con Docker Compose.

```
                                  ┌──────────────────────────────┐
   Cliente (Web / App) ─────────► │      API Gateway :8080       │
                                  │  JWT · CORS · Rate/Retry ·   │
                                  │  Circuit Breaker · Swagger   │
                                  └──────────────┬───────────────┘
                                                 │ lb:// (Eureka)
                    ┌────────────────────────────┼───────────────────────────┐
                    ▼                            ▼                           ▼
        ┌───────────────────────┐  ┌──────────────────────────┐  ┌──────────────────────────┐
        │  auth-service :8081   │  │ contenido-service :8082  │  │ progreso-service :8083   │
        │ registro · login ·    │  │ módulos educativos ·     │  │ progreso de estudiantes ·│
        │ refresh · usuarios ·  │  │ lecciones                │  │ resúmenes y estadísticas │
        │ roles (JWT issuer)    │  │                          │  │          ▲    ▲          │
        └──────────┬────────────┘  └────────────┬─────────────┘  └──────────┼────┼──────────┘
                   │                            │                Feign +   │    │
                   │                            └──────────────────────────┘    │
                   │                                            X-Service-Key  │
                   ▼                                                            ▼
        ┌───────────────────────┐  ┌──────────────────────────┐  ┌──────────────────────────┐
        │ DB: edusecurity_auth  │  │DB:edusecurity_contenido  │  │DB:edusecurity_progreso   │
        │       (MySQL)         │  │       (MySQL)            │  │       (MySQL)            │
        └───────────────────────┘  └──────────────────────────┘  └──────────────────────────┘

   Infraestructura: eureka-server:8761 (descubrimiento) · config-server:8888 (config central)
```

## Microservicios

| Servicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| `api-gateway` | 8080 | Punto único de entrada: JWT, CORS, rutas, Circuit Breaker, Swagger agregado | — |
| `auth-service` | 8081 | Registro, login, refresh token, usuarios y roles (emisor de JWT) | `edusecurity_auth` |
| `contenido-service` | 8082 | Módulos educativos y lecciones (CRUD) | `edusecurity_contenido` |
| `progreso-service` | 8083 | Progreso por lección, resúmenes y estadísticas (consulta vía Feign) | `edusecurity_progreso` |
| `eureka-server` | 8761 | Descubrimiento de servicios | — |
| `config-server` | 8888 | Configuración centralizada compartida | — |
| `edusecurity-common` | — | Librería compartida: seguridad JWT, manejo de errores, paginación, OpenAPI | — |

## Tecnologías

- Java 21 · Spring Boot 3.5 · Spring Cloud 2025.0 (Gateway, Eureka, Config, OpenFeign, Resilience4j)
- Spring Security + JWT (access token 10 h / refresh token 7 días, rotación)
- Spring Data JPA + Flyway (migraciones versionadas) + MySQL 8 (H2 en pruebas)
- springdoc-openapi / Swagger UI
- Docker Compose con healthchecks y orden de arranque por dependencias
- Maven multi-módulo · GitHub Actions (CI)

## Características completas

- **Seguridad**: JWT validado en el Gateway *y* en cada servicio (defensa en profundidad), RBAC con `@PreAuthorize` (`ADMIN`, `ESTUDIANTE`), registro público siempre `ESTUDIANTE` (sin escalada de privilegios), BCrypt, refresh token con rotación, cabecera `X-Service-Key` para llamadas servicio a servicio.
- **Errores estandarizados**: `GlobalExceptionHandler` con formato `{timestamp, status, error, message, path, fieldErrors}` en todos los servicios (404/400/403/401/409/503/500).
- **Validaciones**: `jakarta.validation` en todos los DTOs con detalle por campo.
- **Paginación**: `PageResponse<T>` uniforme (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`) con filtros `q` y ordenamiento.
- **Resiliencia**: Circuit Breaker (Resilience4j) con fallback 503 + reintentos con backoff para GET en el Gateway; timeouts Feign en `progreso-service`.
- **Observabilidad**: Actuator (`health` con probes, `info`) + healthchecks de Docker.
- **Datos**: Flyway con esquemas por servicio, seed idempotente (roles y usuario admin).
- **Documentación**: Swagger por servicio y agregada en el Gateway.
- **Pruebas**: tests unitarios (Mockito), tests de integración (MockMvc + H2) y validación de contexto.
- **CI**: pipeline de compilación + pruebas + validación de `docker-compose.yml`.

## Estructura del proyecto

```text
Backend-Edusecurity/
├── pom.xml                     # Padre Maven (multi-módulo)
├── docker-compose.yml          # Orquestación completa
├── docker/mysql-init/          # Crea los 3 esquemas + usuario dedicado
├── edusecurity-common/         # Librería compartida (auto-config)
├── config-server/              # Config centralizada (config/application.yml)
├── eureka-server/              # Descubrimiento
├── auth-service/               # Auth + usuarios + roles
├── contenido-service/          # Módulos + lecciones
├── progreso-service/           # Progreso + estadísticas (Feign clients)
├── api-gateway/                # Gateway WebFlux
└── .github/workflows/ci.yml    # CI
```

Cada servicio sigue arquitectura por capas:

```text
src/main/java/edusecurity/<servicio>/
├── controller/   # Endpoints REST + @PreAuthorize + OpenAPI
├── service/      # Reglas de negocio + @Transactional
├── repository/   # Spring Data JPA
├── entity/       # JPA
├── dto/          # Requests/Responses (validados, sin fugas de entidades)
└── config|security|client|  # Configuración, seguridad, clientes Feign
src/main/resources/db/migration/  # Migraciones Flyway
```

## Puesta en marcha

### Opción A: Docker Compose (recomendada)

```bash
# 1) Construir los JAR (los Dockerfiles copian target/*.jar)
./mvnw clean package -DskipTests      # Windows: .\mvnw.cmd clean package -DskipTests

# 2) Levantar todo (MySQL, config, eureka, servicios y gateway)
docker compose up --build
```

Servicios disponibles:

| Recurso | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Swagger agregado | http://localhost:8080/swagger-ui.html |
| Eureka | http://localhost:8761 |
| Config Server | http://localhost:8888/application/main |

> Solo el gateway (8080) y Eureka/Config están publicados; los microservicios quedan **aislados** dentro de la red de Docker.

### Opción B: Ejecución local

Orden de arranque (cada uno en una terminal):

```bash
./mvnw -pl config-server spring-boot:run
./mvnw -pl eureka-server spring-boot:run
./mvnw -pl auth-service,contenido-service,progreso-service spring-boot:run
./mvnw -pl api-gateway spring-boot:run
```

Requisitos locales: MySQL 8 ejecutándose (el script `docker/mysql-init/01-databases.sql` crea los esquemas; cada servicio también crea su esquema automáticamente con `createDatabaseIfNotExist=true` y sus tablas con Flyway).

### Credenciales iniciales

| Correo | Contraseña | Rol |
|---|---|---|
| `admin@edusecurity.com` | `Admin123*` | ADMIN |

El rol por defecto de `POST /api/auth/register` es `ESTUDIANTE`.

## Endpoints principales (vía Gateway)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | Público | Registro de estudiante |
| POST | `/api/auth/login` | Público | Devuelve `accessToken` + `refreshToken` |
| POST | `/api/auth/refresh` | Público | Rota el refresh token |
| GET/PUT | `/api/auth/me` | Autenticado | Ver / actualizar mi perfil |
| GET/POST/PUT/DELETE | `/api/usuarios` | ADMIN | CRUD de usuarios, paginado con `q` |
| GET/POST | `/api/roles` | ADMIN | Roles del sistema |
| GET | `/api/modulos`, `/api/lecciones` | Autenticado | Listado paginado con filtros `q`, `moduloId`, `soloActivos` |
| POST/PUT/DELETE | `/api/modulos`, `/api/lecciones` | ADMIN | Gestión de contenido |
| POST | `/api/progreso` | Autenticado | Upsert de progreso (usuario propio; ADMIN: cualquiera) |
| GET | `/api/progreso/mio` | Autenticado | Mi progreso paginado |
| GET | `/api/progreso/resumen/mio` | Autenticado | Mi avance por módulo |
| GET | `/api/progreso/resumen/global` | ADMIN | Estadísticas globales |

Documentación por servicio tras el Gateway: `/openapi/auth/v3/api-docs`, `/openapi/contenido/v3/api-docs`, `/openapi/progreso/v3/api-docs`.

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/edusecurity_<svc>...` | URL de la base de datos del servicio |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / vacío en local; en Docker, `edusecurity` y el valor de `.env` | Credenciales de BD |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Servidor de descubrimiento |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | Servidor de configuración |
| `JWT_SECRET` | **obligatoria, sin valor por defecto** | Clave Base64 de al menos 32 bytes para firmar tokens |
| `INTERNAL_API_KEY` | **obligatoria, sin valor por defecto** | Cabecera `X-Service-Key` servicio a servicio |
| `MYSQL_ROOT_PASSWORD` | **obligatoria en Docker** | Contraseña root del contenedor MySQL |

Los secretos no están en el repositorio. Para Docker, copia `.env.example` a `.env` (ignorado por Git) y genera valores propios:

```bash
cp .env.example .env
openssl rand -base64 48   # JWT_SECRET
openssl rand -hex 24      # INTERNAL_API_KEY y contraseñas
```

Para ejecutar un servicio desde VS Code o IntelliJ, define `JWT_SECRET` e `INTERNAL_API_KEY` en la configuración de ejecución. Sin ellos el servicio no arranca, a propósito.

> ⚠️ No expongas Eureka ni Config Server fuera de la red interna.

## Decisiones de diseño

- **Database por servicio**: cada microservicio posee su esquema y sus migraciones; no hay claves foráneas entre bases de datos, solo IDs cruzados.
- **Sin relaciones JPA entre servicios**: `progreso-service` denormaliza consultando a los otros servicios con **OpenFeign** (+ `X-Service-Key`).
- **JWT doblemente validado**: el Gateway rechaza tokens inválidos antes de enrutar y cada servicio vuelve a validar (nadie puede saltarse el Gateway si llega directamente a la red interna).
- **Config centralizada opcional**: los servicios arrancan solos con sus `application.properties`; el Config Server permite sobreescribir secretos/config compartida en un solo lugar.

## Pruebas y CI

```bash
./mvnw verify          # compila todo + corre las pruebas (usa H2 en memoria)
```

- `auth-service`: flujo completo registro → login → refresh → perfil → control de acceso.
- `contenido-service`: CRUD de módulos/lecciones con validaciones y roles.
- `progreso-service`: upsert, autorización y cálculo de resúmenes (Mockito).
- `edusecurity-common` y `api-gateway`: emisión/validación de JWT.

## Autores

Proyecto desarrollado por:

- Johan Leandro Vega Morales
- Laura Valentina Cardona Vega
- Alejandro Tamayo Zapata
- Juan Manuel Alvarez Ceballos
- Julian David Arias Arrubla

## Ficha

3231660
