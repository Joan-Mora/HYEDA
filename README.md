<p align="center">
  <img src="assets/logo-hea.svg" alt="HEA — Herramientas y Equipos del Ariari" width="96" height="96" />
</p>

<h1 align="center">HYEDA</h1>

<p align="center">
  <strong>Herramientas y Equipos del Ariari</strong><br/>
  Sistema de gestión integral de alquiler de herramientas y maquinaria<br/>
  <em>Granada, Meta · NIT 000.000.000-0</em>
</p>

<p align="center">
  <img src="assets/hyeda-banner.png" alt="Banner HYEDA" width="100%" />
</p>

<p align="center">
  <a href="ARCHITECTURE.md"><img src="https://img.shields.io/badge/Arquitectura-Modular-1B1F4C?style=for-the-badge" alt="Arquitectura" /></a>
  <a href="SECURITY.md"><img src="https://img.shields.io/badge/Seguridad-JWT%20%2B%20RBAC-E65100?style=for-the-badge" alt="Seguridad" /></a>
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/Versión-1.0.0-F4F4F6?style=for-the-badge&labelColor=1B1F4C" alt="Versión" /></a>
</p>

---

## ¿Para qué sirve?

HYEDA es el **centro de control del patio** de *Herramientas y Equipos del Ariari*. Digitaliza el día a día de un negocio de alquiler en Granada (Meta):

| Necesidad del negocio | Qué resuelve HYEDA |
| --- | --- |
| ¿Quién tiene el equipo? | Alquileres en calle, estados y wizard de apertura |
| ¿A dónde va la obra? | Dirección + nombre de referencia (sin depender de coordenadas) |
| ¿Quién debe? | Cartera, abonos, efectivo / QR |
| ¿Qué falta en el patio? | Inventario con stock reservado y alertas |
| ¿Papelería? | Contrato, remisión, devolución y factura en PDF |
| ¿Riesgo del cliente? | Antecedentes / evidencia y reputación |

Pensado para operación real en mostrador: pocos clics, drawer de gestión, atajos (`Ctrl+K`) y flujo guiado de 4 pasos.

---

## Identidad de marca

| Elemento | Valor |
| --- | --- |
| Logo | Monograma **HEA** (círculo navy + tipografía geométrica) — `assets/logo-hea.svg` |
| Navy | `#1B1F4C` |
| Acento | `#E65100` |
| Fondo | `#F4F4F6` / bone |
| Empresa | Herramientas y Equipos del Ariari · Granada, Meta |

---

## Stack tecnológico

### Backend (`backend/`)
- **Java 26** · **Spring Boot 4.1.1**
- Spring Web, Data JPA, Security, Validation
- **JWT** (jjwt) · roles y permisos (RBAC)
- **MapStruct** · Lombok
- Persistencia: **H2** (dev) / **PostgreSQL** (prod)
- PDF: OpenHTMLToPDF · Excel: Apache POI
- Adaptadores mock/reales: WhatsApp, Maps, antecedentes

### Frontend (`frontend/`)
- **React 19** · **TypeScript** · **Vite 8**
- **TanStack Query** · React Router 7 · Axios
- **Tailwind CSS 4** · **Framer Motion**
- **Recharts** (pulso de ingresos)

### Documentación
- [ARCHITECTURE.md](ARCHITECTURE.md) — módulos y flujos
- [SECURITY.md](SECURITY.md) — autenticación, secretos y buenas prácticas
- [CHANGELOG.md](CHANGELOG.md) — historial de versiones
- [docs/INTEGRACIONES.md](docs/INTEGRACIONES.md) — WhatsApp, Wompi, Maps, antecedentes

---

## Estructura del repositorio

```text
HYEDA/
├── assets/           # Logo HEA y banner de marca
├── backend/          # API Spring Boot · puerto 8081
├── frontend/         # SPA React · puerto 5173
├── docs/             # Schema SQL e integraciones
├── README.md
├── ARCHITECTURE.md
├── CHANGELOG.md
└── SECURITY.md
```

---

## Arranque rápido

### 1. Backend

```bash
cd backend
./mvnw spring-boot:run
# Windows: .\mvnw.cmd spring-boot:run
```

- API: `http://localhost:8081/api`
- Perfil default: `dev` (H2 en `./data/hyeda`)
- PostgreSQL: `-Dspring-boot.run.profiles=postgres`

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

- UI: `http://localhost:5173`

### Credenciales de desarrollo

| Usuario | Contraseña |
| --- | --- |
| `admin` | `AdminHyeda2026!` |

> Solo para entorno local / académico. En producción cámbialas con variables de entorno — ver [SECURITY.md](SECURITY.md).

---

## Mapa de la aplicación (UI)

| Ruta | Rol |
| --- | --- |
| **Hoy** | Mesa de mando: KPIs, cola operativa, obras activas |
| **Alquileres** | Hub en calle / historial / documentos + drawer |
| **Clientes** | CRM, cartera, antecedentes |
| **Patio** | Inventario y stock |
| **Caja** | Finanzas y movimientos |
| **Sistema** | Admin, importación, integraciones |

Wizard **Abrir alquiler**: cliente → equipos / fechas / dirección+referencia → firma → pago + WhatsApp.

---

## API (prefijos)

| Prefijo | Descripción |
| --- | --- |
| `/api/auth` | Login JWT |
| `/api/admin` | Usuarios, roles, permisos |
| `/api/clientes` | CRM |
| `/api/productos` | Inventario |
| `/api/alquileres` | Ciclo de vida del alquiler |
| `/api/pagos` | Efectivo / QR / abono |
| `/api/documentos` | PDF |
| `/api/finanzas` | Movimientos y deudas |
| `/api/analytics` | Dashboard + Excel |
| `/api/importacion` | Seed DOCX/XLSX |
| `/api/antecedentes` | Consulta / evidencia |
| `/api/integraciones` | Notificaciones / claves |

---

## Créditos académicos

Proyecto de frameworks / Spring Boot — **Uniminuto**.  
Estudiante: **Darwin Joan Aveiga Mora**.  
Operación de referencia: **Herramientas y Equipos del Ariari** (la propietaria

---

<p align="center">
  <img src="assets/logo-hea.svg" width="40" height="40" alt="HEA" /><br/>
  <sub>HYEDA · Patio bajo control · Granada, Meta</sub>
</p>
