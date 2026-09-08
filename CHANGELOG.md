<p align="center">
  <img src="assets/logo-hea.svg" alt="HEA" width="72" height="72" />
</p>

# CHANGELOG — HYEDA

Historial de cambios del sistema **Herramientas y Equipos del Ariari**.  
Formato inspirado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) · versionado semántico.

---

## [1.1.0] — 2026-09-07

### Añadido
- Dockerfiles multi-stage para backend (Java 26) y frontend (Nginx) orientados a **Render Free**.
- `render.yaml` Blueprint (API + web + Postgres).
- Soporte `PORT`, CORS configurable (`HYEDA_CORS_ORIGINS`) y conversión automática `postgres://` → JDBC.

---

## [1.0.0] — 2026-09-07

Primera entrega pública del producto como **centro de control del patio**.

### Añadido
- Backend modular Spring Boot 4.1.1 (Java 26): auth JWT, clientes, inventario, alquileres, pagos, documentos PDF, finanzas, analytics, importación, antecedentes e integraciones.
- Frontend React 19 + Vite + TypeScript con IA de navegación: **Hoy · Alquileres · Clientes · Patio · Caja · Sistema**.
- Wizard de alquiler en 4 fases: cliente/riesgo → equipos/fechas/obra → firma canvas → pago + WhatsApp.
- Dirección de obra con **nombre de referencia + dirección** (sin dependencia de lat/lng en la UX).
- Lista de obras activas con enlace a Google Maps por texto de búsqueda.
- Drawers (SlideOver), toasts, skeletons, Command Palette (`Ctrl+K`), modal PDF y animaciones Framer Motion.
- Dashboard “Hoy” con KPIs animados, cola operativa y pulso de ingresos (Recharts).
- Seed de usuario operativo de desarrollo y documentación de marca (`assets/logo-hea.svg`, banner).

### Seguridad
- Autenticación JWT + autorización por roles/permisos.
- Secretos y admin configurables por variables de entorno (ver `SECURITY.md`).

### Documentación
- `README.md`, `ARCHITECTURE.md`, `SECURITY.md`, `CHANGELOG.md`.
- `docs/INTEGRACIONES.md` y `docs/schema-hyeda-v2.sql`.

---

## [0.9.0] — 2026-09 (pre-release interno)

### Añadido
- Consolidación de módulos sueltos hacia hub operativo.
- Endurecimiento de `/integraciones` (DTOs de notificaciones).
- Redirecciones de rutas legacy (`/analiticas`, `/antecedentes`, `/admin`, etc.).

### Corregido
- Conflictos de puerto 8081 y pantallas en blanco en integraciones.
- Warnings de Boot / MapStruct / imports no usados.

---

## [0.5.0] — 2026 (laboratorio)

### Añadido
- Primer esqueleto Spring Boot + React para alquiler de herramientas HEA.
- Generación de documentos PDF y flujo básico de reserva/entrega/devolución.

---

## Próximos candidatos

- [ ] Pagos Wompi en modo live (hoy preparado vía integraciones).
- [ ] WhatsApp Business API real (hoy mock/log).
- [ ] Hardening de secretos (sin defaults en perfiles de producción).
- [ ] Pruebas automatizadas de ciclo alquiler → pago → documento.

---

<p align="center">
  <sub>HEA · NIT 000.000.000-0 · Granada, Meta</sub>
</p>
