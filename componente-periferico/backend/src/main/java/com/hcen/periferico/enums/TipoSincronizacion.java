package com.hcen.periferico.enums;

/**
 * Tipo de sincronización pendiente con el componente central.
 * Permite distinguir entre sincronizaciones de usuarios y documentos clínicos.
 */
public enum TipoSincronizacion {
    USUARIO,    // Sincronización de usuario de salud
    DOCUMENTO   // Sincronización de documento clínico
}
