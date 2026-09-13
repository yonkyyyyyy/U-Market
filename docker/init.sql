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
    estado VARCHAR(30) DEFAULT 'DISPONIBLE', -- Ej: 'DISPONIBLE', 'VENDIDO', 'PAUSADO'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    imagen_url VARCHAR(500),          -- URL de imagen medium (600x600)
    thumbnail_url VARCHAR(500),       -- URL de thumbnail (200x200)
    full_url VARCHAR(500)             -- URL de imagen completa
);
CREATE TABLE demandas (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    presupuesto_estimado DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
CREATE TABLE archivos_metadata (
    id SERIAL PRIMARY KEY,
    producto_id INT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    variante VARCHAR(20) NOT NULL, -- 'THUMB', 'MEDIUM', 'FULL'
    nombre_original VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    url VARCHAR(500),
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);