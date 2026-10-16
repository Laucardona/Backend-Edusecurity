# Cómo contribuir a EduSecurity Backend

Guía corta para trabajar en equipo sobre este repositorio.

## Flujo de ramas

```
feature/<tema>  ──PR──▶  develop  ──PR──▶  main
```

- `main`: versión estable. Solo recibe merges desde `develop`.
- `develop`: integración. Todo el trabajo entra aquí por pull request.
- `feature/<tema>`: una rama por historia de usuario o tarea, creada desde `develop`.
  Ejemplos: `feature/hu44-laboratorio-guiado`, `feature/i7-4-resultado-simulaciones-progreso`.
- Nunca hagas push directo a `develop` ni a `main`.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/hu-xx-descripcion-corta
```

## Commits

Usamos [Conventional Commits](https://www.conventionalcommits.org/es/):

```
<tipo>(<alcance opcional>): <descripción en minúscula y en presente>
```

| Tipo | Para qué |
|---|---|
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de un error |
| `refactor` | Cambio interno sin cambiar el comportamiento |
| `test` | Solo pruebas |
| `docs` | Solo documentación |
| `chore` | Mantenimiento: dependencias, configuración, CI |

Ejemplos: `feat(simulacion): laboratorio guiado por pasos (HU44)`, `fix(auth): bloquear tras 5 intentos`.
Menciona la HU o la tarea del plan en el título o en el cuerpo.

## Pull requests

- Abre el PR contra `develop` y completa la plantilla (Antes/Después, Cómo, Pruebas, HU).
- Para hacer merge se necesitan:
  - la **CI en verde** (compilación, pruebas y validación de `docker-compose.yml`);
  - **una aprobación de otro integrante** del equipo (no vale aprobar tu propio PR).
- PRs pequeños y enfocados: una HU o tarea por PR siempre que se pueda.
- Si `develop` avanzó, actualiza tu rama antes del merge y vuelve a correr las pruebas.

## Secretos

- **Nunca subas secretos**: contraseñas, `JWT_SECRET`, `INTERNAL_API_KEY`, credenciales SMTP ni archivos `.env`.
- Los secretos llegan por variables de entorno. Copia `.env.example` a `.env` (ignorado por Git) y pon tus propios valores.
- Si subiste un secreto por error, avisa al equipo de inmediato: hay que rotarlo; borrarlo del commit no basta.

## Pruebas

```bash
./mvnw verify                          # todo el proyecto (Windows: .\mvnw.cmd verify)
./mvnw -pl auth-service -am verify     # un solo servicio y sus dependencias
./mvnw -pl simulacion-service test -Dtest=LaboratorioGuiadoIntegrationTest   # una clase
```

- Las pruebas usan H2 en memoria en modo PostgreSQL; no necesitas una base de datos instalada.
- Todo cambio de comportamiento debe venir con pruebas (unitarias con Mockito o de integración con MockMvc).
- No debilites una aserción existente para que una prueba pase; si el comportamiento cambió a propósito, actualiza la prueba y explícalo en el PR.

## Estilo

- Código, comentarios, mensajes de error y documentación en español, como el resto del proyecto.
- Sigue la estructura por capas de cada servicio (`controller`, `service`, `repository`, `entity`, `dto`).
- Los cambios de esquema van en una migración de Flyway nueva (`V<n>__descripcion.sql`); no edites migraciones que ya estén en `develop` salvo acuerdo del equipo.
