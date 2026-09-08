# Guia de despliegue — HYEDA v2

## Requisitos

- Java 26
- Node 22+
- PostgreSQL 16+ (produccion)
- Variables de entorno listadas en [SECURITY.md](../SECURITY.md)

## Backend

```bash
cd backend
./mvnw.cmd -DskipTests package
java -jar target/hyeda-2.0.0.jar --spring.profiles.active=postgres
```

Flyway aplica `V1__baseline.sql` y `V2__seed_reference_data.sql` al arrancar.
El usuario admin se crea con `HYEDA_ADMIN_USER` / `HYEDA_ADMIN_PASSWORD`.

## Frontend

```bash
cd frontend
npm ci
npm run build
```

Sirve `frontend/dist` con cualquier servidor estatico. `VITE_API_URL` debe apuntar a la API publica (`https://host/api`).

## Perfiles

| Perfil | Base | Uso |
| --- | --- | --- |
| `dev` (default) | H2 archivo `./data/hyeda` | Laboratorio |
| `postgres` | PostgreSQL | Produccion / staging |
| tests | H2 en memoria | `./mvnw test` |

## Jobs

Los cron de reservas, deudas, reporte diario y WhatsApp viven en `hyeda.jobs.*`.
En produccion confirma la zona `America/Bogota`.
