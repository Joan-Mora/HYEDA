-- =============================================================================
-- HYEDA v2 - Datos de referencia
--
-- El usuario administrador NO se crea aqui: lo siembra AdminAccountSeeder para
-- poder cifrar la contrasena con BCrypt a partir de la configuracion.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Permisos, agrupados por modulo de la interfaz
-- -----------------------------------------------------------------------------
INSERT INTO app_permission (code, module, description) VALUES
    ('DASHBOARD_VER',            'Panel',          'Ver el panel operativo del dia'),
    ('INVENTARIO_VER',           'Inventario',     'Consultar productos y stock'),
    ('INVENTARIO_GESTIONAR',     'Inventario',     'Crear, editar y ajustar stock de productos'),
    ('CLIENTES_VER',             'Clientes',       'Consultar clientes y su historial'),
    ('CLIENTES_GESTIONAR',       'Clientes',       'Crear y editar clientes'),
    ('ANTECEDENTES_CONSULTAR',   'Clientes',       'Consultar antecedentes por documento'),
    ('ALQUILERES_VER',           'Alquileres',     'Consultar alquileres'),
    ('ALQUILERES_GESTIONAR',     'Alquileres',     'Abrir, entregar, extender, devolver y cerrar alquileres'),
    ('DOCUMENTOS_VER',           'Documentos',     'Consultar y descargar documentos generados'),
    ('PAGOS_REGISTRAR',          'Facturacion',    'Registrar pagos, abonos y reembolsos'),
    ('FINANZAS_VER',             'Finanzas',       'Consultar movimientos y cartera'),
    ('FINANZAS_GESTIONAR',       'Finanzas',       'Registrar ingresos, gastos y gestionar deudas'),
    ('ANALITICAS_VER',           'Analiticas',     'Consultar metricas y exportar reportes'),
    ('IMPORTACION_EJECUTAR',     'Importacion',    'Cargar archivos CSV y Excel'),
    ('NOTIFICACIONES_GESTIONAR', 'Integraciones',  'Consultar y reenviar notificaciones'),
    ('USUARIOS_GESTIONAR',       'Sistema',        'Crear usuarios, roles y credenciales'),
    ('CONFIGURACION_GESTIONAR',  'Sistema',        'Editar parametros de la empresa e integraciones');

-- -----------------------------------------------------------------------------
-- Roles del sistema
-- -----------------------------------------------------------------------------
INSERT INTO app_role (name, description, system_role) VALUES
    ('ADMINISTRADOR',  'Control total del sistema', TRUE),
    ('OPERADOR_PATIO', 'Atiende el mostrador: abre alquileres, entrega y recibe material', TRUE),
    ('CAJA',           'Registra pagos y consulta cartera', TRUE);

-- El administrador recibe todos los permisos existentes.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM app_role r, app_permission p WHERE r.name = 'ADMINISTRADOR';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r, app_permission p
WHERE r.name = 'OPERADOR_PATIO'
  AND p.code IN ('DASHBOARD_VER', 'INVENTARIO_VER', 'INVENTARIO_GESTIONAR',
                 'CLIENTES_VER', 'CLIENTES_GESTIONAR', 'ANTECEDENTES_CONSULTAR',
                 'ALQUILERES_VER', 'ALQUILERES_GESTIONAR', 'DOCUMENTOS_VER',
                 'PAGOS_REGISTRAR');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM app_role r, app_permission p
WHERE r.name = 'CAJA'
  AND p.code IN ('DASHBOARD_VER', 'CLIENTES_VER', 'ALQUILERES_VER', 'DOCUMENTOS_VER',
                 'PAGOS_REGISTRAR', 'FINANZAS_VER', 'FINANZAS_GESTIONAR', 'ANALITICAS_VER');

-- -----------------------------------------------------------------------------
-- Consecutivos documentales
-- -----------------------------------------------------------------------------
INSERT INTO document_sequence (document_type, prefix, next_value) VALUES
    ('CONTRATO',   'CTR', 1),
    ('REMISION',   'REM', 1),
    ('DEVOLUCION', 'DEV', 1),
    ('FACTURA',    'FAC', 1),
    ('REPORTE',    'REP', 1);

-- -----------------------------------------------------------------------------
-- Datos de la empresa (encabezado de todos los documentos PDF)
-- -----------------------------------------------------------------------------
INSERT INTO company_settings
    (id, legal_name, trade_name, nit, owner_name, tax_regime, phone, email, address, city,
     whatsapp_admin, late_grace_days, daily_report_cron)
VALUES
    (1,
     'Herramientas y Equipos del Ariari',
     'HYEDA',
     '000.000.000-0',
     'la propietaria',
     'Regimen Simplificado - No responsable de IVA',
     '320 408 9057 - 310 666 3764',
     'herramientasyequiposdelariari@gmail.com',
     'Calle 19 No. 12-47, Barrio Antonio Ricaurte',
     'Granada, Meta',
     '570000000000',
     0,
     '0 0 20 * * *');
