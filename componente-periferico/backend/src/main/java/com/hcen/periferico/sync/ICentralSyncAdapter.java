package com.hcen.periferico.sync;

import com.hcen.periferico.entity.UsuarioSalud;

/**
 * Interface para adaptadores de sincronización con el componente central.
 *
 * Patrón Strategy: Permite cambiar la implementación de sincronización
 * sin modificar el código del servicio.
 *
 * Implementaciones:
 * - CentralSyncAdapterREST: Sincronización vía HTTP/REST (actual)
 * - CentralSyncAdapterMQ: Sincronización vía mensajería asíncrona (futuro)
 *
 * Este diseño permite migrar de REST a mensajería (RabbitMQ, ActiveMQ, etc.)
 * sin refactorizar el service layer.
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
}
