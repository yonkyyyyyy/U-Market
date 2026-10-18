CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo_institucional VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL DEFAULT 'ESTUDIANTE', -- Soporta 'ESTUDIANTE', 'ADMINISTRADOR', 'DOCENTE', etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE productos (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10, 2) NOT NULL,
    categoria VARCHAR(80),
    estado VARCHAR(30) DEFAULT 'ACTIVO', -- Ej: 'ACTIVO', 'VENDIDO', 'PAUSADO'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE demandas (
    id SERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    presupuesto_estimado DECIMAL(10, 2),
    categoria VARCHAR(80),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
    usuario_id INT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_demandas_estado CHECK (estado IN ('ACTIVA', 'CUMPLIDA', 'CANCELADA', 'BLOQUEADA'))
);
CREATE TABLE mensajes (
    id SERIAL PRIMARY KEY,
    remitente_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    destinatario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    contenido TEXT NOT NULL,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE chatbot_logs (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    mensaje_usuario TEXT NOT NULL,
    respuesta_ia TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE auditoria_demanda (
    id SERIAL PRIMARY KEY,
    demanda_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL REFERENCES usuarios(id),
    accion VARCHAR(30) NOT NULL,
    motivo TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auditoria_demanda_id ON auditoria_demanda(demanda_id);
CREATE INDEX idx_auditoria_admin_id ON auditoria_demanda(admin_id);

-- Log de cada decisión de la IA de moderación (texto e imagen). Es la fuente del
-- panel de moderación y de la matriz de clasificación: cuando un moderador marca
-- acierto_humano (true = confirma el veredicto de la IA, false = lo corrige), el
-- registro entra a la matriz (VP/FP/FN/VN).
-- OJO: producto_id / usuario_id / revisado_por NO llevan FK a propósito: el log
-- debe sobrevivir al borrado del producto o del usuario (es auditoría), y una FK
-- sin ON DELETE bloquearía ese borrado.
CREATE TABLE moderacion_logs (
    id SERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,              -- 'TEXTO' | 'IMAGEN'
    fuente VARCHAR(20) NOT NULL,            -- 'PUBLICACION' | 'IMAGEN' | 'TTL'
    producto_id BIGINT,
    producto_titulo VARCHAR(150),           -- copia del título (el producto puede desaparecer)
    usuario_id BIGINT,                      -- dueño del contenido moderado
    veredicto VARCHAR(20) NOT NULL,         -- 'APROBADO' | 'RECHAZADO' | 'FALLIDO'
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    categorias VARCHAR(255),                -- categorías detectadas, separadas por coma
    motivo TEXT,                            -- por qué se rechazó / falló (null si aprobado)
    modelo VARCHAR(60),                     -- modelo de IA que decidió (trazabilidad)
    latencia_ms BIGINT NOT NULL DEFAULT 0,  -- duración del análisis
    extracto VARCHAR(300),                  -- extracto del texto revisado (no el texto completo)
    acierto_humano BOOLEAN,                 -- null = sin revisar
    revisado_por BIGINT,                    -- moderador que revisó
    fecha_revision TIMESTAMP,
    comentario_revision TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_moderacion_logs_created_at ON moderacion_logs(created_at);
CREATE INDEX idx_moderacion_logs_producto_id ON moderacion_logs(producto_id);
CREATE INDEX idx_moderacion_logs_tipo_veredicto ON moderacion_logs(tipo, veredicto);