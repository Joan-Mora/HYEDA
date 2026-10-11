<p align="center">
  <img src="assets/logo-hea.svg" alt="HEA" width="72" height="72" />
</p>

# SECURITY — HYEDA

Política de seguridad y buenas prácticas para el sistema **HYEDA**  
(*Herramientas y Equipos del Ariari* · Granada, Meta).

---

## Alcance

Este documento cubre el repositorio académico/operativo de HYEDA: API Spring Boot, SPA React, almacenamiento local de PDFs/evidencias y configuraciones por defecto de desarrollo.

> **Importante:** los valores por defecto existen para laboratorio Uniminuto. **No** deben usarse en internet público sin rotar secretos y endurecer el perfil.

---

## Autenticación y autorización

| Control | Detalle |
| --- | --- |
| Autenticación | Login → **JWT** (Bearer) |
| Almacenamiento token (UI) | Cliente (local); enviar en `Authorization` |
| Autorización | Roles + permisos (RBAC) en endpoints `/api/**` |
| Expiración JWT | Configurable (`hyeda.jwt.expiration-ms`, default 24h) |

Endpoints públicos típicos: login (y recursos estáticos del front en despliegues separados). El resto de `/api` requiere token válido.

---

## Secretos y variables de entorno

Nunca subas `.env`, keystores ni dumps de H2 con datos reales. El `.gitignore` del repo excluye `backend/data/`, `backend/storage/` y archivos `.env*`.

| Variable | Uso |
| --- | --- |
| `HYEDA_JWT_SECRET` | Clave de firma JWT (≥ 256 bits recomendado) |
| `HYEDA_ADMIN_USER` | Usuario seed administrador |
| `HYEDA_ADMIN_PASS` | Contraseña seed administrador |
| `WHATSAPP_ADMIN_PHONE` | Teléfono destino notificaciones (lab) |
| Credenciales DB | Perfil `postgres` (`spring.datasource.*`) |

Defaults de desarrollo (solo local):

```text
HYEDA_ADMIN_USER=admin
HYEDA_ADMIN_PASS=AdminHyeda2026!
HYEDA_JWT_SECRET=<valor largo definido en application-*.properties>
```

**Producción:** obliga variables de entorno, desactiva H2 console, usa HTTPS y PostgreSQL con usuario de privilegio mínimo.

---

## Datos sensibles del negocio

HYEDA puede almacenar:

- Datos de clientes (documento, teléfono, dirección)
- Consentimiento de tratamiento de datos
- Copias de documento / evidencias de antecedentes (archivos en `storage/`)
- Historial de alquileres, pagos y deudas

Recomendaciones:
1. Minimizar retención de evidencias PDF.
2. Restringir roles que ven antecedentes y documentos de identidad.
3. No exponer `storage/` ni H2 console fuera de localhost.
4. Cumplir políticas de privacidad acordadas con el titular del negocio (HEA).

---

## Superficie de ataque y mitigaciones

| Riesgo | Mitigación en HYEDA |
| --- | --- |
| Acceso no autenticado a API | Spring Security + JWT |
| Escalada de privilegios | Permisos por endpoint / rol |
| Secretos en git | `.gitignore` + env vars |
| Path traversal en archivos | Servir solo rutas controladas por el backend |
| XSS en SPA | React escapa por defecto; evitar `dangerouslySetInnerHTML` con datos crudos |
| CSRF en API JWT | API stateless; no cookies de sesión clásicas |
| Dependencias | Mantener Boot / npm actualizados |

Integraciones (WhatsApp, Maps, pasarela): usar claves en servidor, nunca embebidas en el bundle del frontend para operaciones privilegiadas. Ver `docs/INTEGRACIONES.md`.

---

## Checklist antes de demos / despliegue

- [ ] Cambiar `HYEDA_ADMIN_PASS` y `HYEDA_JWT_SECRET`
- [ ] Confirmar que `backend/data/` y `backend/storage/` no se publican
- [ ] Perfil `postgres` con SSL si aplica
- [ ] CORS limitado al origen del frontend
- [ ] Revisar teléfonos y datos seed (no usar números personales en repos públicos si no es necesario)
- [ ] Desactivar DevTools restart / H2 console en prod

---

## Reporte de hallazgos

Si encuentras una vulnerabilidad en este proyecto académico, contacta al mantenedor del repositorio (estudiante Uniminuto / equipo HEA) de forma privada antes de divulgar detalles explotables.

---

## Relación con la marca

La identidad visual (logo HEA navy `#1B1F4C` + acento `#E65100`) no implica certificación de seguridad externa. Este archivo describe controles implementados y pendientes para un uso responsable del software.

---

<p align="center">
  <img src="assets/logo-hea.svg" width="36" height="36" alt="HEA" /><br/>
  <sub>HYEDA Security · Patio bajo control · Granada, Meta</sub>
</p>
