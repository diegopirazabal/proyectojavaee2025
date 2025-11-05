package com.hcen.periferico.sync;

import com.hcen.periferico.api.CentralAPIClient;
import com.hcen.periferico.entity.UsuarioSalud;
import com.hcen.periferico.enums.TipoDocumento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación del adaptador de sincronización usando REST/HTTP.
 *
 * Esta implementación usa el CentralAPIClient para comunicarse con el
 * componente central vía HTTP.
 *
 * NOTA: Cuando se implemente mensajería asíncrona, se creará una nueva
 * implementación (CentralSyncAdapterMQ) sin modificar esta clase.
 */
@Stateless
public class CentralSyncAdapterREST implements ICentralSyncAdapter {

    private static final Logger LOGGER = Logger.getLogger(CentralSyncAdapterREST.class.getName());

    @EJB
    private CentralAPIClient centralClient;

    @Override
    public SyncResult enviarUsuario(UsuarioSalud usuario) {
        try {
            LOGGER.info("Sincronizando usuario con central via REST: " + usuario.getCedula());

            // Convertir tipo documento String a enum (si es necesario)
            TipoDocumento tipoDoc = parseTipoDocumento(usuario.getTipoDocumento());

            // IMPORTANTE: Enviamos SIN tenant_id al central (usuario único global)
            // El central almacenará el usuario sin tenant_id
            centralClient.registrarUsuarioEnClinica(
                usuario.getCedula(),
                tipoDoc,
                usuario.getPrimerNombre(),
                usuario.getSegundoNombre(),
                usuario.getPrimerApellido(),
                usuario.getSegundoApellido(),
                usuario.getEmail(),
                usuario.getFechaNacimiento(),
                null  // tenant_id = null, ya que central no lo necesita
            );

            LOGGER.info("Usuario sincronizado exitosamente con central");
            return SyncResult.exitoso("Usuario registrado en componente central");

        } catch (RuntimeException e) {
            // Verificar si el error es porque el usuario ya existe
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Status: 200")) {
                // Status 200 con mensaje de "ya existe" es éxito
                LOGGER.info("Usuario ya existía en central: " + usuario.getCedula());
                return SyncResult.exitosoYaExistia("Usuario ya existía en componente central");
            } else if (errorMsg != null && (errorMsg.contains("ya está registrado") ||
                                            errorMsg.contains("already exists"))) {
                // Mensaje explícito de duplicado también es éxito
                LOGGER.info("Usuario ya existía en central: " + usuario.getCedula());
                return SyncResult.exitosoYaExistia("Usuario ya existía en componente central");
            }

            // Error real de comunicación o validación
            LOGGER.log(Level.SEVERE, "Error al sincronizar usuario con central", e);
            return SyncResult.fallido(
                "Error al comunicarse con el componente central",
                e.getMessage()
            );
        }
    }

    @Override
    public boolean verificarUsuarioExiste(String cedula) {
        try {
            return centralClient.verificarUsuarioExiste(cedula);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Error al verificar existencia de usuario en central", e);
            // En caso de error de comunicación, asumimos que no existe
            // (fail-safe: permitir registro local)
            return false;
        }
    }

    @Override
    public String getNombre() {
        return "REST";
    }

    /**
     * Parsea el tipo de documento de String a enum.
     * Si es null o vacío, retorna DO (Documento de identidad) por defecto.
     */
    private TipoDocumento parseTipoDocumento(String tipoDocStr) {
        if (tipoDocStr == null || tipoDocStr.isEmpty()) {
            return TipoDocumento.DO;
        }
        try {
            return TipoDocumento.valueOf(tipoDocStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Tipo de documento no válido: " + tipoDocStr + ", usando DO por defecto");
            return TipoDocumento.DO;
        }
    }
}
