-- =============================================================================
-- HYEDA — Modelo Relacional v2 (PostgreSQL)
-- SaaS interno: inventario, alquileres, CRM, finanzas, documentos, omicanal
-- Aislamiento de stock: SELECT ... FOR UPDATE + stock_reservation + stock_ledger
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. RBAC
-- -----------------------------------------------------------------------------
CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(80)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150),
    email           VARCHAR(120),
    phone           VARCHAR(40),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    description TEXT,
    is_system   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(80)  NOT NULL UNIQUE, -- ej. INVENTARIO, ALQUILERES, ANALITICAS
    module      VARCHAR(80)  NOT NULL,        -- agrupación UI
    description TEXT
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permission (
    role_id       BIGINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- -----------------------------------------------------------------------------
-- 2. CRM / Reputación / Antecedentes
-- -----------------------------------------------------------------------------
CREATE TYPE customer_type AS ENUM ('NUEVO', 'FRECUENTE', 'CONFIANZA');

CREATE TABLE customer (
    id                 BIGSERIAL PRIMARY KEY,
    document           VARCHAR(40)  NOT NULL UNIQUE,
    document_type      VARCHAR(20)  NOT NULL DEFAULT 'CC',
    name               VARCHAR(180) NOT NULL,
    phone              VARCHAR(40),
    email              VARCHAR(120),
    id_copy_path       VARCHAR(500),          -- adjunto único de cédula
    data_consent       BOOLEAN NOT NULL DEFAULT FALSE,
    data_consent_at    TIMESTAMPTZ,
    reputation_score   NUMERIC(8,2) NOT NULL DEFAULT 50.00,
    customer_type      customer_type NOT NULL DEFAULT 'NUEVO',
    notes              TEXT,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE customer_address (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    label        VARCHAR(40) NOT NULL DEFAULT 'OBRA', -- OBRA | RESIDENCIA | OFICINA
    address      TEXT NOT NULL,
    lat          DOUBLE PRECISION,
    lng          DOUBLE PRECISION,
    maps_url     VARCHAR(500),
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE reputation_event (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    rental_id    BIGINT, -- FK diferida (circular con rental)
    event_type   VARCHAR(40) NOT NULL, -- EXITO | MORA | DANIO | ABONO | MANUAL
    delta        NUMERIC(8,2) NOT NULL,
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE background_check (
    id             BIGSERIAL PRIMARY KEY,
    customer_id    BIGINT REFERENCES customer(id) ON DELETE SET NULL,
    document       VARCHAR(40) NOT NULL,
    result         TEXT,
    status         VARCHAR(30) NOT NULL DEFAULT 'PENDING_MANUAL',
    -- PENDING_MANUAL | CLEAR | ALERT | REJECTED | SUBMITTED
    provider       VARCHAR(80) NOT NULL DEFAULT 'manual',
    evidence_path  VARCHAR(500),
    observations   TEXT,
    checked_by     BIGINT REFERENCES app_user(id),
    checked_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_background_document ON background_check(document);

-- -----------------------------------------------------------------------------
-- 3. Inventario + concurrencia
-- -----------------------------------------------------------------------------
CREATE TABLE product (
    id                   BIGSERIAL PRIMARY KEY,
    sku                  VARCHAR(40) UNIQUE,
    name                 VARCHAR(200) NOT NULL,
    description          TEXT,
    category             VARCHAR(80),
    daily_price          NUMERIC(14,2) NOT NULL CHECK (daily_price >= 0),
    late_fee_per_day     NUMERIC(14,2) NOT NULL DEFAULT 0, -- recargo explícito
    stock_total          INTEGER NOT NULL DEFAULT 0 CHECK (stock_total >= 0),
    stock_available      INTEGER NOT NULL DEFAULT 0 CHECK (stock_available >= 0),
    min_stock_threshold  INTEGER NOT NULL DEFAULT 1,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    version              BIGINT NOT NULL DEFAULT 0, -- optimistic lock (@Version)
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_stock_available_le_total CHECK (stock_available <= stock_total)
);

CREATE TYPE stock_movement_type AS ENUM (
    'IMPORT', 'ADJUST', 'RESERVE', 'RELEASE', 'DISPATCH', 'RETURN', 'DAMAGE_WRITEOFF'
);

CREATE TABLE stock_ledger (
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT NOT NULL REFERENCES product(id),
    rental_id     BIGINT,
    movement_type stock_movement_type NOT NULL,
    quantity      INTEGER NOT NULL, -- + entrada / - salida
    balance_after INTEGER NOT NULL,
    note          TEXT,
    user_id       BIGINT REFERENCES app_user(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_ledger_product ON stock_ledger(product_id, created_at DESC);

CREATE TABLE stock_reservation (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product(id),
    rental_id   BIGINT, -- se llena al crear borrador
    session_key VARCHAR(80), -- id de wizard en cliente (prioridad FIFO)
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservation_active ON stock_reservation(product_id, consumed, expires_at);

-- -----------------------------------------------------------------------------
-- 4. Alquiler (máquina de estados)
-- -----------------------------------------------------------------------------
CREATE TYPE rental_status AS ENUM (
    'BORRADOR',
    'RESERVADO',
    'DOCUMENTO_EMITIDO',
    'EN_CURSO',
    'DEVUELTO',
    'FACTURACION_FINAL',
    'PENDIENTE_PAGO',
    'CERRADO',
    'CANCELADO'
);

CREATE TYPE rental_document_policy AS ENUM ('CONTRATO', 'REMISION');

CREATE TABLE rental (
    id                   BIGSERIAL PRIMARY KEY,
    customer_id          BIGINT NOT NULL REFERENCES customer(id),
    user_id              BIGINT REFERENCES app_user(id),
    status               rental_status NOT NULL DEFAULT 'BORRADOR',
    document_policy      rental_document_policy NOT NULL DEFAULT 'CONTRATO',
    skip_contract        BOOLEAN NOT NULL DEFAULT FALSE,
    start_date           DATE NOT NULL,
    end_date             DATE NOT NULL,
    actual_return_date   DATE,
    estimated_days       INTEGER NOT NULL CHECK (estimated_days > 0),
    actual_days          INTEGER,
    delivery_address     TEXT,
    delivery_lat         DOUBLE PRECISION,
    delivery_lng         DOUBLE PRECISION,
    delivery_maps_url    VARCHAR(500),
    hauling_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    hauling_cost         NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_amount      NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_reason      TEXT,
    estimated_total      NUMERIC(14,2) NOT NULL DEFAULT 0,
    final_amount         NUMERIC(14,2),
    credit_applied       NUMERIC(14,2) NOT NULL DEFAULT 0, -- saldo a favor usado
    refund_amount        NUMERIC(14,2) NOT NULL DEFAULT 0, -- devolución de dinero
    data_consent_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_discount_reason CHECK (
        discount_amount = 0 OR (discount_reason IS NOT NULL AND length(trim(discount_reason)) > 0)
    )
);

ALTER TABLE stock_reservation
    ADD CONSTRAINT fk_reservation_rental FOREIGN KEY (rental_id) REFERENCES rental(id) ON DELETE SET NULL;

ALTER TABLE stock_ledger
    ADD CONSTRAINT fk_ledger_rental FOREIGN KEY (rental_id) REFERENCES rental(id) ON DELETE SET NULL;

ALTER TABLE reputation_event
    ADD CONSTRAINT fk_reputation_rental FOREIGN KEY (rental_id) REFERENCES rental(id) ON DELETE SET NULL;

CREATE TABLE rental_item (
    id              BIGSERIAL PRIMARY KEY,
    rental_id       BIGINT NOT NULL REFERENCES rental(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES product(id),
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(14,2) NOT NULL, -- snapshot
    late_fee_snapshot NUMERIC(14,2) NOT NULL DEFAULT 0,
    estimated_days  INTEGER NOT NULL,
    actual_days     INTEGER,
    line_estimated  NUMERIC(14,2) NOT NULL DEFAULT 0,
    line_final      NUMERIC(14,2)
);

CREATE TABLE rental_extension (
    id           BIGSERIAL PRIMARY KEY,
    rental_id    BIGINT NOT NULL REFERENCES rental(id) ON DELETE CASCADE,
    extra_days   INTEGER NOT NULL CHECK (extra_days > 0),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved_by  BIGINT REFERENCES app_user(id)
);

CREATE TABLE rental_status_history (
    id          BIGSERIAL PRIMARY KEY,
    rental_id   BIGINT NOT NULL REFERENCES rental(id) ON DELETE CASCADE,
    from_status rental_status,
    to_status   rental_status NOT NULL,
    changed_by  BIGINT REFERENCES app_user(id),
    note        TEXT,
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE return_incident (
    id           BIGSERIAL PRIMARY KEY,
    rental_id    BIGINT NOT NULL REFERENCES rental(id) ON DELETE CASCADE,
    rental_item_id BIGINT REFERENCES rental_item(id),
    condition    VARCHAR(40) NOT NULL, -- OK | DANIO | FALTANTE | INCONFORMIDAD
    reason       VARCHAR(120) NOT NULL,
    description  TEXT,
    charge_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    reported_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reported_by  BIGINT REFERENCES app_user(id)
);

-- -----------------------------------------------------------------------------
-- 5. Pagos / Deudas / Crédito (saldo a favor)
-- -----------------------------------------------------------------------------
CREATE TYPE payment_type AS ENUM ('EFECTIVO', 'QR', 'ABONO', 'REEMBOLSO', 'AJUSTE');
CREATE TYPE payment_status AS ENUM ('PENDIENTE', 'CONFIRMADO', 'FALLIDO', 'ANULADO');

CREATE TABLE payment (
    id              BIGSERIAL PRIMARY KEY,
    rental_id       BIGINT NOT NULL REFERENCES rental(id),
    payment_type    payment_type NOT NULL,
    amount          NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    amount_tendered NUMERIC(14,2), -- efectivo recibido
    change_given    NUMERIC(14,2) NOT NULL DEFAULT 0,
    status          payment_status NOT NULL DEFAULT 'PENDIENTE',
    reference       VARCHAR(120),
    provider        VARCHAR(40), -- mock | wompi | nequi
    provider_payload JSONB,
    paid_at         TIMESTAMPTZ,
    recorded_by     BIGINT REFERENCES app_user(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE debt_status AS ENUM ('ABIERTA', 'PARCIAL', 'PAGADA', 'VENCIDA', 'CONDONADA');

CREATE TABLE debt (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT NOT NULL REFERENCES customer(id),
    rental_id     BIGINT REFERENCES rental(id),
    balance       NUMERIC(14,2) NOT NULL CHECK (balance >= 0),
    original_amount NUMERIC(14,2) NOT NULL,
    due_date      DATE,
    overdue_days  INTEGER NOT NULL DEFAULT 0,
    status        debt_status NOT NULL DEFAULT 'ABIERTA',
    last_notice_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE customer_credit (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customer(id),
    rental_id    BIGINT REFERENCES rental(id),
    amount       NUMERIC(14,2) NOT NULL, -- + crédito / - uso
    reason       TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 6. Documentos
-- -----------------------------------------------------------------------------
CREATE TYPE generated_document_type AS ENUM (
    'CONTRATO', 'REMISION', 'DEVOLUCION', 'FACTURA', 'REPORTE', 'CEDULA'
);

CREATE TABLE document_template (
    id            BIGSERIAL PRIMARY KEY,
    document_type generated_document_type NOT NULL,
    name          VARCHAR(120) NOT NULL,
    version       INTEGER NOT NULL DEFAULT 1,
    html_body     TEXT NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE generated_document (
    id             BIGSERIAL PRIMARY KEY,
    rental_id      BIGINT REFERENCES rental(id) ON DELETE SET NULL,
    customer_id    BIGINT REFERENCES customer(id) ON DELETE SET NULL,
    document_type  generated_document_type NOT NULL,
    serial_number  VARCHAR(40) NOT NULL UNIQUE,
    file_path      VARCHAR(500),
    template_id    BIGINT REFERENCES document_template(id),
    metadata_json  JSONB,
    signed         BOOLEAN NOT NULL DEFAULT FALSE,
    signed_at      TIMESTAMPTZ,
    generated_by   BIGINT REFERENCES app_user(id),
    generated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE document_sequence (
    document_type generated_document_type PRIMARY KEY,
    next_value    BIGINT NOT NULL DEFAULT 1,
    prefix        VARCHAR(10) NOT NULL
);

-- -----------------------------------------------------------------------------
-- 7. Finanzas empresa
-- -----------------------------------------------------------------------------
CREATE TYPE movement_type AS ENUM ('INGRESO', 'GASTO');

CREATE TABLE financial_movement (
    id             BIGSERIAL PRIMARY KEY,
    movement_type  movement_type NOT NULL,
    amount         NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    description    TEXT,
    category       VARCHAR(80),
    movement_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    rental_id      BIGINT REFERENCES rental(id),
    user_id        BIGINT REFERENCES app_user(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 8. Omnicanal / notificaciones
-- -----------------------------------------------------------------------------
CREATE TABLE notification (
    id           BIGSERIAL PRIMARY KEY,
    rental_id    BIGINT REFERENCES rental(id),
    customer_id  BIGINT REFERENCES customer(id),
    channel      VARCHAR(40) NOT NULL DEFAULT 'WHATSAPP',
    template_code VARCHAR(80),
    recipient    VARCHAR(40),
    message      TEXT,
    media_path   VARCHAR(500), -- PDF adjunto
    status       VARCHAR(40) NOT NULL DEFAULT 'QUEUED',
    -- QUEUED | SENT | FAILED | READ
    provider_response JSONB,
    sent_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_status ON notification(status, created_at);

-- -----------------------------------------------------------------------------
-- 9. Auditoría / empresa / importaciones
-- -----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES app_user(id),
    action      VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id   BIGINT,
    before_json JSONB,
    after_json  JSONB,
    ip_address  VARCHAR(60),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE company_settings (
    id              SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    legal_name      VARCHAR(200) NOT NULL,
    trade_name      VARCHAR(200) NOT NULL DEFAULT 'HYEDA',
    nit             VARCHAR(40) NOT NULL,
    phone           VARCHAR(40),
    address         TEXT,
    logo_path       VARCHAR(500),
    whatsapp_admin  VARCHAR(40),
    timezone        VARCHAR(60) NOT NULL DEFAULT 'America/Bogota',
    daily_report_cron VARCHAR(40) NOT NULL DEFAULT '0 0 20 * * *'
);

CREATE TABLE import_job (
    id           BIGSERIAL PRIMARY KEY,
    job_type     VARCHAR(40) NOT NULL, -- CUSTOMERS | PRODUCTS | MOVEMENTS | DEBTS
    file_name    VARCHAR(255),
    status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rows_ok      INTEGER NOT NULL DEFAULT 0,
    rows_error   INTEGER NOT NULL DEFAULT 0,
    error_log    TEXT,
    started_by   BIGINT REFERENCES app_user(id),
    started_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at  TIMESTAMPTZ
);

-- Seed mínimo de secuencias documentales
INSERT INTO document_sequence (document_type, next_value, prefix) VALUES
    ('CONTRATO', 1, 'CTR'),
    ('REMISION', 1, 'REM'),
    ('DEVOLUCION', 1, 'DEV'),
    ('FACTURA', 1, 'FAC'),
    ('REPORTE', 1, 'REP')
ON CONFLICT DO NOTHING;
