package com.hcen.periferico.sync;

import com.hcen.periferico.api.CentralAPIClient;
import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.entity.UsuarioSalud;
import com.hcen.periferico.entity.documento_clinico;
import com.hcen.periferico.enums.TipoSincronizacion;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementación del adaptador de sincronización de documentos clínicos usando REST/HTTP.
 *
 * Este adapter sincroniza documentos clínicos de un usuario con el componente central.
 * Para cada documento sin hist_clinica_id (no sincronizado), llama al endpoint
 * POST /historia-clinica/documentos del componente central.
 *
 * Patrón Strategy: Permite separar la lógica de sincronización de documentos
 * de la lógica de usuarios, manteniendo la misma interfaz.
 */
@Stateless
public class CentralSyncAdapterDocumentos implements ICentralSyncAdapter {

    private static final Logger LOGGER = Logger.getLogger(CentralSyncAdapterDocumentos.class.getName());

    @EJB
    private CentralAPIClient centralClient;

    @EJB
    private DocumentoClinicoDAO documentoDAO;

    /**
     * Sincroniza todos los documentos clínicos pendientes de un usuario con el componente central.
     *
     * IMPORTANTE: Este método busca TODOS los documentos del usuario que no tienen hist_clinica_id
     * (campo NULL indica que no han sido sincronizados) y los envía al componente central.
     *
     * @param usuario Usuario cuyos documentos se deben sincronizar
     * @return SyncResult indicando cuántos documentos se sincronizaron exitosamente
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public SyncResult enviarUsuario(UsuarioSalud usuario) {
        try {
            String cedula = usuario.getCedula();
            UUID tenantId = usuario.getTenantId();

            LOGGER.info("Sincronizando documentos clínicos del usuario " + cedula + " con central via REST");

            // Buscar todos los documentos del usuario
            List<documento_clinico> todosDocumentos = documentoDAO.findByPaciente(cedula, tenantId);

            // Filtrar solo los que NO están sincronizados (hist_clinica_id = NULL)
            List<documento_clinico> documentosPendientes = todosDocumentos.stream()
                .filter(doc -> doc.getHistClinicaId() == null)
                .collect(Collectors.toList());

            if (documentosPendientes.isEmpty()) {
                LOGGER.info("No hay documentos pendientes de sincronización para usuario " + cedula);
                return SyncResult.exitoso("No hay documentos pendientes de sincronización");
            }

            LOGGER.info("Encontrados " + documentosPendientes.size() + " documentos pendientes de sincronización");

            int sincronizadosExitosos = 0;
            int errores = 0;
            StringBuilder erroresDetalle = new StringBuilder();

            // Sincronizar cada documento pendiente
            for (documento_clinico doc : documentosPendientes) {
                try {
                    UUID historiaId = centralClient.registrarDocumentoHistoriaClinica(
                        cedula,
                        tenantId,
                        doc.getId()
                    );

                    // Actualizar el campo hist_clinica_id con el ID retornado por el central
                    doc.setHistClinicaId(historiaId);
                    documentoDAO.save(doc);

                    sincronizadosExitosos++;
                    LOGGER.info("Documento " + doc.getId() + " sincronizado exitosamente con historia ID " + historiaId);

                } catch (Exception e) {
                    errores++;
                    String errorMsg = "Documento " + doc.getId() + ": " + e.getMessage();
                    erroresDetalle.append(errorMsg).append("; ");
                    LOGGER.log(Level.WARNING, "Error al sincronizar documento " + doc.getId(), e);
                }
            }

            // Evaluar resultado
            if (sincronizadosExitosos > 0 && errores == 0) {
                return SyncResult.exitoso(
                    "Sincronizados " + sincronizadosExitosos + " documentos exitosamente"
                );
            } else if (sincronizadosExitosos > 0 && errores > 0) {
                return SyncResult.fallido(
                    "Sincronizados " + sincronizadosExitosos + " documentos, " + errores + " fallaron",
                    erroresDetalle.toString()
                );
            } else {
                return SyncResult.fallido(
                    "No se pudo sincronizar ningún documento",
                    erroresDetalle.toString()
                );
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error crítico al sincronizar documentos del usuario", e);
            return SyncResult.fallido(
                "Error crítico al sincronizar documentos",
                e.getMessage()
            );
        }
    }

    /**
     * Verifica si un usuario existe en el componente central.
     *
     * NOTA: Este método no aplica para la sincronización de documentos,
     * ya que los documentos se sincronizan solo si el usuario ya existe.
     * Se incluye para cumplir con la interfaz ICentralSyncAdapter.
     *
     * @param cedula Cédula del usuario a verificar
     * @return true si existe en central
     * @throws UnsupportedOperationException Este adapter no soporta esta operación
     */
    @Override
    public boolean verificarUsuarioExiste(String cedula) {
        // Para documentos, asumimos que el usuario ya debe existir en el central
        // antes de intentar sincronizar documentos
        try {
            return centralClient.verificarUsuarioExiste(cedula);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Error al verificar existencia de usuario en central", e);
            return false;
        }
    }

    @Override
    public String getNombre() {
        return "Documentos-REST";
    }

    @Override
    public TipoSincronizacion getTipo() {
        return TipoSincronizacion.DOCUMENTO;
    }
}
