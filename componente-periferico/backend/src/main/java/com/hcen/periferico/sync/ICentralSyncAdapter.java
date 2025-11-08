package com.hcen.periferico.sync;

import com.hcen.periferico.entity.UsuarioSalud;
import com.hcen.periferico.enums.TipoSincronizacion;

/**
 * Interface para adaptadores de sincronización con el componente central.
 *
 * Patrón Strategy: Permite cambiar la implementación de sincronización
 * sin modificar el código del servicio.
 *
 * Tipos de sincronización:
 * - USUARIO: Sincronización de usuarios de salud
 * - DOCUMENTO: Sincronización de documentos clínicos
 *
 * Implementaciones actuales:
 * - CentralSyncAdapterREST: Sincronización de usuarios vía HTTP/REST
 * - CentralSyncAdapterDocumentos: Sincronización de documentos vía HTTP/REST
 *
 * Este diseño permite:
 * - Separar la lógica de sincronización de usuarios y documentos
 * - Migrar a mensajería asíncrona (RabbitMQ, ActiveMQ) sin refactorizar
 * - Procesar reintentos de forma independiente por tipo
 */
public interface ICentralSyncAdapter {

    /**
     * Envía un usuario al componente central para su registro global.
     *
     * @param usuario Usuario a sincronizar (con datos locales del periférico)
     * @return SyncResult indicando si fue exitoso, si ya existía, y mensaje/errores
     */
    SyncResult enviarUsuario(UsuarioSalud usuario);

    /**
     * Verifica si un usuario existe en el componente central.
     *
     * @param cedula Cédula del usuario a verificar
     * @return true si existe en central, false si no
     */
    boolean verificarUsuarioExiste(String cedula);

    /**
     * Obtiene el nombre del adapter (para logging/debugging).
     *
     * @return Nombre del adapter (ej: "REST", "MQ")
     */
    String getNombre();

    /**
     * Obtiene el tipo de sincronización que maneja este adapter.
     * Permite al sistema de reintentos filtrar por tipo (USUARIO o DOCUMENTO).
     *
     * @return TipoSincronizacion (USUARIO o DOCUMENTO)
     */
    TipoSincronizacion getTipo();
}
