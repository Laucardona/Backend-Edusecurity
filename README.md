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
        │     (PostgreSQL)      │  │     (PostgreSQL)         │  │     (PostgreSQL)         │
        └───────────────────────┘  └──────────────────────────┘  └──────────────────────────┘

   Infraestructura: eureka-server:8761 (descubrimiento) · config-server:8888 (config central)
```

## Microservicios

| Servicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| `api-gateway` | 8080 | Punto único de entrada: JWT, CORS, rutas, Circuit Breaker, Swagger agregado | — |
| `auth-service` | 8081 | Registro, login, refresh token, usuarios y roles (emisor de JWT) | `edusecurity_auth` |
| `contenido-service` | 8082 | Módulos educativos y lecciones (CRUD) | `edusecurity_contenido` |
| `progreso-service` | 8083 | Progreso por lección, resúmenes, estadísticas y panel administrativo (módulo `analitica`, HU33) | `edusecurity_progreso` |
| `simulacion-service` | 8084 | Laboratorios de phishing con bandeja simulada (HU43) y laboratorios virtuales guiados por pasos (HU44), con tiempo límite y retroalimentación | `edusecurity_simulacion` |
| `eureka-server` | 8761 | Descubrimiento de servicios | — |
| `config-server` | 8888 | Configuración centralizada compartida | — |
| `edusecurity-common` | — | Librería compartida: seguridad JWT, manejo de errores, paginación, OpenAPI | — |

## Tecnologías

- Java 21 · Spring Boot 3.5 · Spring Cloud 2025.0 (Gateway, Eureka, Config, OpenFeign, Resilience4j)
- Spring Security + JWT (access token 10 h / refresh token 7 días, rotación)
- Spring Data JPA + Flyway (migraciones versionadas) + PostgreSQL 16 (H2 en modo PostgreSQL en pruebas)
- springdoc-openapi / Swagger UI
- Docker Compose con healthchecks y orden de arranque por dependencias
- Maven multi-módulo · GitHub Actions (CI)

## Características completas

- **Accesos (M01)**: política de contraseñas (10+ caracteres con mayúscula, minúscula, número y especial), bloqueo temporal tras 5 intentos fallidos, sesiones persistidas con cierre de sesión e historial, y recuperación de contraseña por correo con token de un solo uso.
- **Seguridad**: JWT validado en el Gateway *y* en cada servicio (defensa en profundidad), RBAC con `@PreAuthorize` (`ADMIN`, `ESTUDIANTE`), registro público siempre `ESTUDIANTE` (sin escalada de privilegios), BCrypt, refresh token con rotación, cabecera `X-Service-Key` para llamadas servicio a servicio.
- **Errores estandarizados**: `GlobalExceptionHandler` con formato `{timestamp, status, error, message, path, fieldErrors}` en todos los servicios (404/400/403/401/409/503/500).
- **Validaciones**: `jakarta.validation` en todos los DTOs con detalle por campo.
- **Paginación**: `PageResponse<T>` uniforme (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`) con filtros `q` y ordenamiento.
- **Resiliencia**: Circuit Breaker (Resilience4j) con fallback 503 + reintentos con backoff para GET en el Gateway; timeouts Feign en `progreso-service` y `simulacion-service`; envío de resultados de simulaciones después del commit con reintento programado (outbox ligero).
- **Observabilidad**: Actuator (`health` con probes, `info`) + healthchecks de Docker.
- **Datos**: PostgreSQL 16 con una base y un rol por servicio, Flyway por servicio, seed idempotente (roles y usuario admin).
- **Documentación**: Swagger por servicio y agregada en el Gateway.
- **Pruebas**: tests unitarios (Mockito), tests de integración (MockMvc + H2) y validación de contexto.
- **CI**: pipeline de compilación + pruebas + validación de `docker-compose.yml`.

## Estructura del proyecto

```text
Backend-Edusecurity/
├── pom.xml                     # Padre Maven (multi-módulo)
├── docker-compose.yml          # Orquestación completa
├── docker/postgres-init/       # Crea una base y un rol por servicio
├── edusecurity-common/         # Librería compartida (auto-config)
├── config-server/              # Config centralizada (config/application.yml)
├── eureka-server/              # Descubrimiento
├── auth-service/               # Auth + usuarios + roles
├── contenido-service/          # Módulos + lecciones
├── progreso-service/           # Progreso + estadísticas (Feign clients)
├── simulacion-service/         # Laboratorios de phishing (HU43) y guiados (HU44)
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

# 2) Levantar todo (PostgreSQL, config, eureka, servicios y gateway)
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
./mvnw -pl auth-service,contenido-service,progreso-service,simulacion-service spring-boot:run
./mvnw -pl api-gateway spring-boot:run
```

Requisitos locales: PostgreSQL 16 ejecutándose, con las bases `edusecurity_auth`, `edusecurity_contenido`, `edusecurity_progreso` y `edusecurity_simulacion` y sus roles (`auth_svc`, ...). Lo más simple es levantar solo la base con Docker (`docker compose up -d postgres`), que ejecuta `docker/postgres-init/01-databases.sh`. Flyway crea las tablas al arrancar cada servicio. Define `DB_PASSWORD` con la contraseña del rol del servicio.

### Credenciales iniciales

No hay contraseña de administrador por defecto. El administrador inicial se crea al arrancar
auth-service solo si defines `ADMIN_PASSWORD` (y opcionalmente `ADMIN_CORREO`, por defecto
`admin@edusecurity.com`). Sin `ADMIN_PASSWORD` no se crea y queda una advertencia en el log;
la contraseña nunca se escribe en los logs. Cámbiala después del primer ingreso.

El rol por defecto de `POST /api/auth/register` es `ESTUDIANTE`.

## Endpoints principales (vía Gateway)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | Público | Registro de estudiante |
| POST | `/api/auth/login` | Público | Devuelve `accessToken` + `refreshToken` |
| POST | `/api/auth/refresh` | Público | Rota el refresh token (cada refresh token sirve una sola vez) |
| POST | `/api/auth/logout` | Autenticado | Cierra la sesión del `refreshToken` enviado, o todas con `{"todas":true}` |
| GET | `/api/auth/sesiones` | Autenticado | Historial de mis inicios de sesión (fecha, IP, dispositivo) |
| POST | `/api/auth/password/recuperar` | Público | Envía un enlace de recuperación al correo (responde 202 siempre) |
| POST | `/api/auth/password/restablecer` | Público | Cambia la contraseña con el token del correo y cierra todas las sesiones |
| POST | `/api/auth/password/fortaleza` | Público | Indicador de fortaleza: puntaje 0 a 4, nivel y requisitos faltantes |
| GET/PUT | `/api/auth/me` | Autenticado | Ver / actualizar mi perfil |
| GET/POST/PUT/DELETE | `/api/usuarios` | ADMIN | CRUD de usuarios, paginado con `q` |
| GET/POST | `/api/roles` | ADMIN | Roles del sistema |
| GET | `/api/modulos`, `/api/lecciones` | Autenticado | Listado paginado con filtros `q`, `moduloId`, `soloActivos` |
| POST/PUT/DELETE | `/api/modulos`, `/api/lecciones` | ADMIN | Gestión de contenido |
| POST | `/api/progreso` | Autenticado | Upsert de progreso (usuario propio; ADMIN: cualquiera) |
| GET | `/api/progreso/mio` | Autenticado | Mi progreso paginado |
| GET | `/api/progreso/simulaciones/mias` | Autenticado | Mis simulaciones terminadas (phishing y laboratorios guiados), las más recientes primero |
| GET | `/api/progreso/resumen/mio` | Autenticado | Mi avance por módulo, con `simulacionesCompletadas` y `simulacionesAprobadas` |
| GET | `/api/progreso/resumen/global` | ADMIN | Estadísticas globales |
| GET | `/api/analitica/panel?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | ADMIN | Panel administrativo (HU33): usuarios y simulaciones del periodo, más el estado actual del progreso en lecciones (por defecto los últimos 30 días, máximo 366). `certificados` es `null` hasta implementar RF22. Si auth-service no responde, `usuarios` es `null` y aparece en `fuentesNoDisponibles` |
| GET | `/api/simulaciones`, `/api/simulaciones/{uuid}`, `/api/simulaciones/indicadores` | Autenticado | Catálogo de escenarios (filtro `tipo`: phishing o `LABORATORIO`) y señales de alerta |
| POST | `/api/simulaciones/{uuid}/sesiones` | Autenticado | Iniciar un laboratorio (bandeja o primer paso + tiempo límite) |
| PUT | `/api/simulaciones/sesiones/{sesion}/respuestas/{mensajeId}` | Dueño | Phishing: clasificar un mensaje, elegir acción y marcar señales (400 en un laboratorio guiado) |
| POST | `/api/simulaciones/sesiones/{sesion}/finalizar` | Dueño | Terminar y recibir la retroalimentación (en un laboratorio guiado, el recorrido paso a paso) |
| GET | `/api/simulaciones/sesiones/{sesion}/paso` | Dueño o ADMIN | Laboratorio guiado: paso actual con su recurso simulado y opciones, sin puntos ni respuesta correcta |
| POST | `/api/simulaciones/sesiones/{sesion}/acciones` | Dueño | Laboratorio guiado: `{pasoCodigo, opcionCodigo}` → retroalimentación y siguiente paso, o `finalizada` (409 si no es el paso actual, 400 si la opción no es del paso) |
| POST/PATCH | `/api/simulaciones`, `/api/simulaciones/{uuid}/publicacion` | ADMIN | Crear (por ahora solo phishing; los laboratorios guiados se cargan por migración) y publicar escenarios |

Endpoints internos (servicio a servicio, solo con `X-Service-Key`, rol `INTERNAL`). El Gateway solo enruta `/api/**`, así que `/internal/**` no es accesible desde fuera; no agregues rutas `/internal/**` al Gateway.

| Método | Ruta | Servicio | Descripción |
|---|---|---|---|
| POST | `/internal/progreso/simulaciones` | progreso-service | Registra el resultado de una simulación terminada. Idempotente por `sesionUuid`: 201 al crear, 200 con el registro existente si se repite |
| GET | `/internal/usuarios/metricas?desde&hasta` | auth-service | Métricas para el panel: total, activos, por rol, bloqueados ahora, usuarios con sesión e inicios de sesión en el periodo. Los usuarios no guardan fecha de creación, así que no hay "nuevos en el periodo" (queda como posible migración futura) |

### Flujo simulación → progreso (I7.4)

1. Una sesión se cierra en `simulacion-service` (finalizar, último paso de un laboratorio guiado o vencimiento con estado `VENCIDA`).
2. Después del commit (`@TransactionalEventListener(AFTER_COMMIT)`), `NotificadorProgreso` envía el resultado a `POST /internal/progreso/simulaciones` por Feign (Eureka `edusecurity-progreso`, cabecera `X-Service-Key`) y marca la sesión con `progreso_notificado = true`.
3. Si progreso-service no responde, el estudiante igual recibe su respuesta: se registra una advertencia y la sesión queda pendiente.
4. Una tarea `@Scheduled` reenvía las sesiones cerradas pendientes en lotes pequeños (cada `PROGRESO_REINTENTO_MS`, 60 s por defecto). Como el registro es idempotente, un reenvío nunca duplica datos.
5. El estudiante consulta su historial en `/api/progreso/simulaciones/mias`, y `/api/progreso/resumen/mio` incluye `simulacionesCompletadas` y `simulacionesAprobadas`.

Es un outbox ligero hasta el outbox completo de I2.2. Las sesiones que vencen sin que nadie las vuelva a consultar se cierran (y se envían) la próxima vez que se accede a ellas.

Documentación por servicio tras el Gateway: `/openapi/auth/v3/api-docs`, `/openapi/contenido/v3/api-docs`, `/openapi/progreso/v3/api-docs`, `/openapi/simulacion/v3/api-docs`.

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/edusecurity_<svc>` | URL de la base de datos del servicio |
| `DB_USERNAME` / `DB_PASSWORD` | `<svc>_svc` / vacío | Rol y contraseña de la base del servicio. En Docker salen de `DB_PASSWORD_<SVC>` |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Servidor de descubrimiento |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | Servidor de configuración |
| `JWT_SECRET` | **obligatoria, sin valor por defecto** | Clave Base64 de al menos 32 bytes para firmar tokens |
| `INTERNAL_API_KEY` | **obligatoria, sin valor por defecto** | Cabecera `X-Service-Key` servicio a servicio |
| `PROGRESO_REINTENTO_MS` | `60000` | Cada cuánto `simulacion-service` reenvía a progreso-service los resultados pendientes |
| `POSTGRES_PASSWORD` | **obligatoria en Docker** | Contraseña del superusuario `postgres` del contenedor |
| `DB_PASSWORD_AUTH` / `DB_PASSWORD_CONTENIDO` / `DB_PASSWORD_PROGRESO` / `DB_PASSWORD_SIMULACION` | **obligatorias en Docker** | Contraseña del rol de cada servicio (una distinta por servicio) |
| `JWT_ACCESS_EXPIRATION` | `15m` | Vigencia del access token (corta para que el cierre de sesión sea efectivo) |
| `ACCESO_MAX_INTENTOS` / `ACCESO_DURACION_BLOQUEO` | `5` / `15m` | Intentos fallidos que bloquean la cuenta y por cuánto tiempo |
| `ACCESO_VIGENCIA_RECUPERACION` | `30m` | Vigencia del enlace de recuperación de contraseña |
| `ACCESO_URL_RESTABLECER` | `http://localhost:5500/restablecer.html` | Página del frontend que recibe `?token=` |
| `ADMIN_CORREO` / `ADMIN_PASSWORD` | `admin@edusecurity.com` / vacío | Administrador inicial. Sin `ADMIN_PASSWORD` no se crea |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | vacío / `587` | SMTP para los correos. Sin `MAIL_HOST` no se envía ningún correo |

Los secretos no están en el repositorio. Para Docker, copia `.env.example` a `.env` (ignorado por Git) y genera valores propios:

```bash
cp .env.example .env
openssl rand -base64 48   # JWT_SECRET
openssl rand -hex 24      # INTERNAL_API_KEY y contraseñas
```

Para ejecutar un servicio desde VS Code o IntelliJ, define `JWT_SECRET` e `INTERNAL_API_KEY` en la configuración de ejecución. Sin ellos el servicio no arranca, a propósito.

> ⚠️ No expongas Eureka ni Config Server fuera de la red interna.

## Decisiones de diseño

- **Database por servicio (ADR-002)**: cada microservicio tiene su propia base de PostgreSQL y sus migraciones; no hay claves foráneas entre bases de datos, solo IDs cruzados. Cada servicio se conecta con su propio rol (`auth_svc`, `contenido_svc`, `progreso_svc`, `simulacion_svc`), dueño solo de su base y sin acceso a las demás (`REVOKE ALL ... FROM PUBLIC`): mínimo privilegio, de modo que un servicio comprometido no expone los datos de los otros.
- **PostgreSQL en lugar de MySQL**: el 2026-10-15, a solicitud de quien lidera el proyecto, el equipo cambió la decisión de ADR-002 de MySQL 8 a PostgreSQL 16. Las migraciones de Flyway se editaron en el lugar (nada estaba en producción), así que cada entorno debe empezar con una base vacía.
- **Sin relaciones JPA entre servicios**: `progreso-service` denormaliza consultando a los otros servicios con **OpenFeign** (+ `X-Service-Key`).
- **Analítica dentro de progreso-service**: el panel administrativo (HU33) vive en el paquete `edusecurity.progreso.analitica`, no en un servicio nuevo. ADR-001 y el diseño permiten que analítica empiece como módulo de formación/progreso y se extraiga después sin cambiar contratos: el Gateway ya enruta `/api/analitica/**` aparte (`analitica-ruta`), así que extraerlo solo cambia el `uri`.
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
