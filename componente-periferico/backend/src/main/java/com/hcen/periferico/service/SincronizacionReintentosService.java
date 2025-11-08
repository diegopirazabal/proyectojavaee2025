package com.hcen.periferico.service;

import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.dao.SincronizacionPendienteDAO;
import com.hcen.periferico.entity.SincronizacionPendiente;
import com.hcen.periferico.entity.documento_clinico;
import com.hcen.periferico.enums.TipoSincronizacion;
import com.hcen.periferico.messaging.DocumentoSincronizacionProducer;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.jms.JMSException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio encargado de reintentar la sincronización de documentos clínicos que
 * quedaron en estado ERROR dentro de la tabla sincronizacion_pendiente.
 *
 * Se utiliza desde endpoints administrativos para forzar el reenvío manual de
 * documentos a la cola JMS procesada por el componente central.
 */
@Stateless
public class SincronizacionReintentosService {

    private static final Logger LOGGER = Logger.getLogger(SincronizacionReintentosService.class.getName());
    private static final int MAX_INTENTOS_DEFAULT = 5;

    @EJB
    private SincronizacionPendienteDAO sincronizacionDAO;

    @EJB
    private DocumentoClinicoDAO documentoDAO;

    @EJB
    private DocumentoSincronizacionProducer documentoProducer;

    /**
     * Reenvía los documentos que siguen pendientes o con errores hacia la cola
     * JMS. Solo se consideran aquellos registros que aún no superan el límite
     * de intentos configurado.
     *
     * @return cantidad de documentos que se volvieron a encolar
     */
    public int procesarInmediato() {
        List<SincronizacionPendiente> candidatos =
            sincronizacionDAO.findByTipoParaReintentar(TipoSincronizacion.DOCUMENTO, MAX_INTENTOS_DEFAULT);

        int reenviados = 0;

        for (SincronizacionPendiente pendiente : candidatos) {
            if (!requiereReintento(pendiente)) {
                continue;
            }

            try {
                if (reenviarDocumento(pendiente)) {
                    reenviados++;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                        "Error inesperado reintentando sincronización {0}: {1}",
                        new Object[]{pendiente.getId(), e.getMessage()});
                pendiente.registrarError("Error inesperado en reintento: " + e.getMessage());
                sincronizacionDAO.save(pendiente);
            }
        }

        LOGGER.log(Level.INFO,
                "Reintentos ejecutados. reenviados={0}, total_candidatos={1}",
                new Object[]{reenviados, candidatos.size()});

        return reenviados;
    }

    private boolean requiereReintento(SincronizacionPendiente pendiente) {
        if (pendiente == null) {
            return false;
        }

        if (pendiente.getDocumentoId() == null) {
            LOGGER.log(Level.WARNING,
                    "Registro de sincronización sin documento_id. Marcando como CANCELADA. id={0}",
                    pendiente.getId());
            pendiente.marcarComoCancelada();
            pendiente.setUltimoError("Documento asociado no existe");
            sincronizacionDAO.save(pendiente);
            return false;
        }

        if (pendiente.getTenantId() == null) {
            LOGGER.log(Level.WARNING,
                    "Registro de sincronización sin tenantId. Marcando como CANCELADA. id={0}",
                    pendiente.getId());
            pendiente.marcarComoCancelada();
            pendiente.setUltimoError("Tenant asociado no existe");
            sincronizacionDAO.save(pendiente);
            return false;
        }

        if (pendiente.getEstado() == SincronizacionPendiente.EstadoSincronizacion.RESUELTA ||
            pendiente.getEstado() == SincronizacionPendiente.EstadoSincronizacion.CANCELADA) {
            return false;
        }

        // Solo reintentar errores o envíos que nunca llegaron a encolarse
        return pendiente.getEstado() == SincronizacionPendiente.EstadoSincronizacion.ERROR ||
               pendiente.getMessageId() == null;
    }

    private boolean reenviarDocumento(SincronizacionPendiente pendiente) throws JMSException {
        Optional<documento_clinico> documentoOpt =
            documentoDAO.findByIdAndTenantId(pendiente.getDocumentoId(), pendiente.getTenantId());

        if (documentoOpt.isEmpty()) {
            LOGGER.log(Level.WARNING,
                    "Documento {0} no existe localmente. Cancelando sincronización.",
                    pendiente.getDocumentoId());
            pendiente.marcarComoCancelada();
            pendiente.setUltimoError("Documento inexistente en base local");
            sincronizacionDAO.save(pendiente);
            return false;
        }

        documento_clinico documento = documentoOpt.get();
        String messageId = documentoProducer.enviarDocumento(
                documento.getId(),
                documento.getUsuarioSaludCedula(),
                documento.getTenantId()
        );

        pendiente.incrementarIntentos();
        pendiente.setEstado(SincronizacionPendiente.EstadoSincronizacion.PENDIENTE);
        pendiente.setUltimoError(null);
        pendiente.setMessageId(messageId);
        pendiente.setFecEnvioCola(LocalDateTime.now());
        sincronizacionDAO.save(pendiente);

        LOGGER.log(Level.INFO,
                "Documento {0} reenviado a cola. messageId={1}",
                new Object[]{documento.getId(), messageId});

        return true;
    }
}
