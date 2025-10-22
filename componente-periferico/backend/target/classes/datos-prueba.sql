-- Script de datos de prueba para el componente periférico multi-tenancy
-- HCEN - Historia Clínica Electrónica Nacional
--
-- Este script crea las tablas necesarias y carga datos de prueba

-- =====================================================
-- CREACIÓN DE TABLAS
-- =====================================================

-- Eliminar tablas existentes (solo para desarrollo/testing)
DROP TABLE IF EXISTS ADMINISTRADOR_CLINICA CASCADE;
DROP TABLE IF EXISTS CONFIGURACION_CLINICA CASCADE;
DROP TABLE IF EXISTS PROFESIONAL_SALUD CASCADE;

-- Tabla: ADMINISTRADOR_CLINICA
-- Administradores de clínicas con sistema multi-tenancy
CREATE TABLE ADMINISTRADOR_CLINICA (
    ID UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    USERNAME VARCHAR(80) NOT NULL,
    PASSWORD VARCHAR(200) NOT NULL,
    NOMBRE VARCHAR(100) NOT NULL,
    APELLIDOS VARCHAR(100) NOT NULL,
    CLINICA_RUT VARCHAR(20) NOT NULL,
    CONSTRAINT UK_ADMIN_USERNAME_CLINICA UNIQUE (USERNAME, CLINICA_RUT)
);

-- Tabla: CONFIGURACION_CLINICA
-- Configuración personalizada por clínica (look & feel y funcional)
CREATE TABLE CONFIGURACION_CLINICA (
    ID UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    CLINICA_RUT VARCHAR(20) NOT NULL UNIQUE,
    COLOR_PRIMARIO VARCHAR(7),
    COLOR_SECUNDARIO VARCHAR(7),
    LOGO_URL VARCHAR(500),
    NOMBRE_SISTEMA VARCHAR(150),
    TEMA VARCHAR(50),
    NODO_PERIFERICO_HABILITADO BOOLEAN NOT NULL DEFAULT FALSE
);

-- Tabla: PROFESIONAL_SALUD
-- Profesionales de salud que pueden trabajar en múltiples clínicas
CREATE TABLE PROFESIONAL_SALUD (
    CI INTEGER PRIMARY KEY,
    NOMBRE VARCHAR(100) NOT NULL,
    APELLIDOS VARCHAR(100) NOT NULL,
    ESPECIALIDAD VARCHAR(80),
    EMAIL VARCHAR(150)
);

-- Índices para mejorar rendimiento
CREATE INDEX IDX_ADMIN_USERNAME ON ADMINISTRADOR_CLINICA(USERNAME);
CREATE INDEX IDX_ADMIN_CLINICA_RUT ON ADMINISTRADOR_CLINICA(CLINICA_RUT);
CREATE INDEX IDX_CONFIG_CLINICA_RUT ON CONFIGURACION_CLINICA(CLINICA_RUT);
CREATE INDEX IDX_PROFESIONAL_NOMBRE ON PROFESIONAL_SALUD(NOMBRE, APELLIDOS);

-- =====================================================
-- DATOS DE PRUEBA
-- =====================================================

-- =====================================================
-- ADMINISTRADORES DE CLÍNICA (con BCrypt password hash)
-- Password para todos: Admin123
-- Hash generado con BCrypt (12 rounds)
-- =====================================================

-- Administrador para Clínica Médica del Este (RUT: 211234560018)
INSERT INTO ADMINISTRADOR_CLINICA (id, username, password, nombre, apellidos, clinica_rut)
VALUES (
    gen_random_uuid(),
    'admin.este',
    '$2a$12$77//pxT1Gm7HJIPB5LpruOu8YMqa4GjcjNo9QyME.CX/W0cseLOAe', -- Admin123
    'Carlos',
    'Rodríguez',
    '211234560018'
);

-- Administrador para Clínica San José (RUT: 217654320014)
INSERT INTO ADMINISTRADOR_CLINICA (id, username, password, nombre, apellidos, clinica_rut)
VALUES (
    gen_random_uuid(),
    'admin.sanjose',
    '$2a$12$77//pxT1Gm7HJIPB5LpruOu8YMqa4GjcjNo9QyME.CX/W0cseLOAe', -- Admin123
    'María',
    'González',
    '217654320014'
);

-- Administrador para Laboratorio Central (RUT: 218765430019)
INSERT INTO ADMINISTRADOR_CLINICA (id, username, password, nombre, apellidos, clinica_rut)
VALUES (
    gen_random_uuid(),
    'admin.labcentral',
    '$2a$12$77//pxT1Gm7HJIPB5LpruOu8YMqa4GjcjNo9QyME.CX/W0cseLOAe', -- Admin123
    'Pedro',
    'Martínez',
    '218765430019'
);

-- Ejemplo de administrador que trabaja en dos clínicas (mismo username, diferentes clínicas)
INSERT INTO ADMINISTRADOR_CLINICA (id, username, password, nombre, apellidos, clinica_rut)
VALUES (
    gen_random_uuid(),
    'juan.perez',
    '$2a$12$77//pxT1Gm7HJIPB5LpruOu8YMqa4GjcjNo9QyME.CX/W0cseLOAe', -- Admin123
    'Juan',
    'Pérez',
    '211234560018'
);

INSERT INTO ADMINISTRADOR_CLINICA (id, username, password, nombre, apellidos, clinica_rut)
VALUES (
    gen_random_uuid(),
    'juan.perez',
    '$2a$12$77//pxT1Gm7HJIPB5LpruOu8YMqa4GjcjNo9QyME.CX/W0cseLOAe', -- Admin123
    'Juan',
    'Pérez',
    '217654320014'
);

-- =====================================================
-- CONFIGURACIONES DE CLÍNICA
-- =====================================================

-- Configuración para Clínica Médica del Este
INSERT INTO CONFIGURACION_CLINICA (id, clinica_rut, color_primario, color_secundario, nombre_sistema, tema, nodo_periferico_habilitado, logo_url)
VALUES (
    gen_random_uuid(),
    '211234560018',
    '#1976d2',
    '#424242',
    'Clínica Médica del Este',
    'saga',
    true,
    NULL
);

-- Configuración para Clínica San José
INSERT INTO CONFIGURACION_CLINICA (id, clinica_rut, color_primario, color_secundario, nombre_sistema, tema, nodo_periferico_habilitado, logo_url)
VALUES (
    gen_random_uuid(),
    '217654320014',
    '#c62828',
    '#6a1b9a',
    'Clínica San José',
    'arya',
    true,
    NULL
);

-- Configuración para Laboratorio Central (modo aislado)
INSERT INTO CONFIGURACION_CLINICA (id, clinica_rut, color_primario, color_secundario, nombre_sistema, tema, nodo_periferico_habilitado, logo_url)
VALUES (
    gen_random_uuid(),
    '218765430019',
    '#2e7d32',
    '#f57c00',
    'Laboratorio Central de Análisis',
    'vela',
    false,
    NULL
);

-- =====================================================
-- PROFESIONALES DE SALUD DE EJEMPLO
-- =====================================================

-- Profesionales para Clínica Médica del Este
INSERT INTO PROFESIONAL_SALUD (ci, nombre, apellidos, especialidad, email)
VALUES
    (12345678, 'Ana', 'García López', 'Cardiología', 'ana.garcia@clinicaeste.com.uy'),
    (23456789, 'Roberto', 'Silva Fernández', 'Pediatría', 'roberto.silva@clinicaeste.com.uy'),
    (34567890, 'Laura', 'Méndez Castro', 'Medicina General', 'laura.mendez@clinicaeste.com.uy');

-- Profesionales para Clínica San José
INSERT INTO PROFESIONAL_SALUD (ci, nombre, apellidos, especialidad, email)
VALUES
    (45678901, 'Diego', 'Ramírez Sosa', 'Traumatología', 'diego.ramirez@clinicasanjose.com.uy'),
    (56789012, 'Carolina', 'Vázquez Ríos', 'Ginecología', 'carolina.vazquez@clinicasanjose.com.uy');

-- Profesionales para Laboratorio Central
INSERT INTO PROFESIONAL_SALUD (ci, nombre, apellidos, especialidad, email)
VALUES
    (67890123, 'Fernando', 'Castro Núñez', 'Análisis Clínicos', 'fernando.castro@labcentral.com.uy'),
    (78901234, 'Sofía', 'Romero Díaz', 'Bioquímica', 'sofia.romero@labcentral.com.uy');

-- =====================================================
-- NOTAS DE USO
-- =====================================================

-- Para iniciar sesión en el sistema:
-- Usuario: admin.este
-- Password: Admin123
-- RUT Clínica: 211234560018

-- Usuario: admin.sanjose
-- Password: Admin123
-- RUT Clínica: 217654320014

-- Usuario: admin.labcentral
-- Password: Admin123
-- RUT Clínica: 218765430019

-- Usuario multi-clínica: juan.perez
-- Password: Admin123
-- RUT Clínica: 211234560018 ó 217654320014
