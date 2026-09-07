# HYEDA Frontend

React + Vite + TypeScript UI for the HYEDA rental management system.

## Setup

```bash
npm install
cp .env.example .env
npm run dev
```

- App: http://localhost:5173  
- API: `http://localhost:8081/api` (`VITE_API_URL`)
- Login (backend): `admin` / `AdminHyeda2026!`

## Stack

- React Router (sidebar layout + protected routes)
- TanStack Query + Axios (`hyeda_token` Bearer interceptor)
- Tailwind CSS v4 — dark slate `#1a1f24` + amber `#e0892e`
- Recharts for analytics

## Modules

Dashboard, Clientes, Inventario, Alquileres (wizard), Documentos (PDF blob), Finanzas, Analíticas, Admin, Importación.
