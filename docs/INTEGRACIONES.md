# HYEDA — Checklist de integraciones reales

Este documento lista las credenciales y pasos para activar WhatsApp, pagos QR, Google Maps y antecedentes en producción.

## 1. WhatsApp Cloud API (Meta)

1. Crear/verificar Meta Business Manager.
2. Crear app tipo **Business** y agregar producto **WhatsApp**.
3. Obtener:
   - `WHATSAPP_TOKEN` (token permanente del sistema)
   - `WHATSAPP_PHONE_NUMBER_ID`
   - `WHATSAPP_BUSINESS_ACCOUNT_ID` (opcional, para plantillas)
4. Número admin para reportes: `WHATSAPP_ADMIN_PHONE=0000000000` (o el de la empresa).
5. En `application.properties` / variables de entorno:
   ```
   hyeda.whatsapp.enabled=true
   hyeda.whatsapp.token=...
   hyeda.whatsapp.phone-number-id=...
   hyeda.whatsapp.admin-phone=57XXXXXXXXXX
   ```
6. Aprobar plantillas en Meta (resumen alquiler, factura, reporte diario) si usas template messages.

**Dev:** con `enabled=false` los mensajes se registran en log y tabla `notification` (mock).

## 2. Pagos QR (Colombia) — Wompi recomendado

1. Crear cuenta en [Wompi](https://wompi.co/) (Bancolombia).
2. Obtener llaves de sandbox/producción:
   - `WOMPI_PUBLIC_KEY`
   - `WOMPI_PRIVATE_KEY`
   - `WOMPI_INTEGRITY_KEY`
3. Configurar webhook público HTTPS hacia `/api/pagos/webhook` (ampliar cuando se active producción).
4. Variables:
   ```
   hyeda.payments.provider=wompi
   hyeda.payments.wompi.public-key=...
   hyeda.payments.wompi.private-key=...
   hyeda.payments.wompi.integrity-key=...
   ```

**Dev:** `provider=mock` genera referencia QR simulada; confirmar con `POST /api/pagos/{id}/confirmar-qr`.

Alternativas: ePayco, Nequi/PSE según preferencia comercial.

## 3. Google Maps

1. Proyecto en Google Cloud con facturación.
2. Activar APIs: Maps JavaScript, Geocoding (Places opcional).
3. Crear API key restringida por dominio HTTP.
4. Variable:
   ```
   GOOGLE_MAPS_API_KEY=...
   hyeda.maps.api-key=${GOOGLE_MAPS_API_KEY}
   ```
5. Frontend: `GET /api/integraciones/maps-key` expone si está configurada (y la key para el mapa embebido en wizard).

## 4. Antecedentes penales

**Nota legal:** no existe API pública policial gratuita en Colombia para terceros.

Opciones soportadas por el adapter:

| Modo | Uso |
|------|-----|
| `manual` (default) | Registrar consulta + subir PDF certificado + observaciones |
| Proveedor (Verifik, CheckID, etc.) | Contratar API comercial y setear `BACKGROUND_CHECK_API_KEY` |

```
hyeda.background-check.provider=manual
hyeda.background-check.api-key=
```

Endpoints:
- `POST /api/antecedentes/consultar`
- `POST /api/antecedentes/{id}/evidencia`
- `GET /api/antecedentes/cliente/{customerId}`

Requiere checkbox de consentimiento de datos del cliente (`dataConsent`).

## 5. Hosting producción

- PostgreSQL (`spring.profiles.active=postgres`)
- Redis (recomendado para locks de stock; hoy hay fallback in-memory)
- Volumen persistente para `hyeda.storage.path`
- HTTPS + dominio (obligatorio para webhooks WhatsApp/Wompi)
- Variables secretas fuera del repositorio (Railway/Render/AWS Secrets)

## 6. Credenciales admin iniciales

```
hyeda.admin.username=admin
hyeda.admin.password=AdminHyeda2026!
```

Cambiar inmediatamente en producción.

## 7. Checklist rápido

- [ ] Meta Business + WhatsApp token
- [ ] Wompi / ePayco llaves + webhook
- [ ] Google Maps API key restringida
- [ ] Decisión proveedor antecedentes o flujo manual
- [ ] PostgreSQL + backups
- [ ] Dominio HTTPS
- [ ] Rotación password admin
