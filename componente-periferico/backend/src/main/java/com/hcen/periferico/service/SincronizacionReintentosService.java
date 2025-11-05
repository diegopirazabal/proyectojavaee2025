package com.hcen.periferico.service;

import com.hcen.periferico.api.CentralAPIClient;
import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.dao.SincronizacionPendienteDAO;
import com.hcen.periferico.entity.SincronizacionPendiente;
import com.hcen.periferico.entity.documento_clinico;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio programado para reintentar sincronizaciones pendientes con el componente central.
 * Se ejecuta cada 15 minutos procesando documentos que no se pudieron sincronizar inicialmente.
 */
@Singleton
@Startup
public class SincronizacionReintentosService {

    private static final int MAX_INTENTOS = 5;
    private static final int BATCH_SIZE = 50;

    @EJB
    private SincronizacionPendienteDAO sincronizacionDAO;

    @EJB
    private DocumentoClinicoDAO documentoDAO;

    @EJB
    private CentralAPIClient centralAPIClient;

    /**
     * Procesa reintentos de sincronización cada 15 minutos.
     * Se ejecuta automáticamente gracias a @Schedule.
     */
    @Schedule(minute = "*/15", hour = "*", persistent = false)
    public void procesarReintentos() {
        System.out.println("=== Iniciando procesamiento de reintentos de sincronización ===");

        try {
            // Obtener sincronizaciones pendientes o con error (con límite de intentos)
            List<SincronizacionPendiente> pendientes =
                sincronizacionDAO.findParaReintentar(MAX_INTENTOS);

            if (pendientes.isEmpty()) {
                System.out.println("No hay sincronizaciones pendientes para procesar.");
                return;
            }

            System.out.println("Encontradas " + pendientes.size() + " sincronizaciones pendientes. " +
                             "Procesando hasta " + BATCH_SIZE + "...");

            int procesados = 0;
            int exitosos = 0;
            int fallidos = 0;

            // Procesar batch limitado
            for (SincronizacionPendiente sync : pendientes) {
                if (procesados >= BATCH_SIZE) {
                    break;
                }

                procesados++;
                boolean exito = procesarSincronizacion(sync);

                if (exito) {
                    exitosos++;
                } else {
                    fallidos++;
                }
            }

            System.out.println("=== Procesamiento completado: " +
                             "Procesados=" + procesados +
                             ", Exitosos=" + exitosos +
                             ", Fallidos=" + fallidos + " ===");

        } catch (Exception e) {
            System.err.println("Error en el procesamiento de reintentos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa una sincronización individual.
     * @return true si tuvo éxito, false si falló
     */
    private boolean procesarSincronizacion(SincronizacionPendiente sync) {
        try {
            System.out.println("Procesando sincronización para usuario " +
                             sync.getUsuarioCedula() + " (intento " +
                             (sync.getIntentos() + 1) + "/" + MAX_INTENTOS + ")");

            // Buscar todos los documentos no sincronizados de este usuario
            List<documento_clinico> documentos =
                documentoDAO.findByPaciente(sync.getUsuarioCedula(), sync.getTenantId());

            boolean algunoSincronizado = false;

            for (documento_clinico doc : documentos) {
                // Solo procesar documentos sin histClinicaId
                if (doc.getHistClinicaId() == null) {
                    boolean sincronizado = sincronizarDocumento(doc, sync.getTenantId());
                    if (sincronizado) {
                        algunoSincronizado = true;
                    }
                }
            }

            if (algunoSincronizado) {
                // Si al menos un documento se sincronizó, marcar como resuelto
                sync.marcarComoResuelta();
                sincronizacionDAO.save(sync);
                System.out.println("✓ Sincronización exitosa para usuario " + sync.getUsuarioCedula());
                return true;
            } else {
                // Si ningún documento se pudo sincronizar, registrar error
                sync.registrarError("No se pudo sincronizar ningún documento en el intento " +
                                   (sync.getIntentos()));
                sincronizacionDAO.save(sync);
                System.out.println("✗ Sincronización falló para usuario " + sync.getUsuarioCedula());
                return false;
            }

        } catch (Exception e) {
            // Error inesperado, registrar y continuar
            sync.registrarError("Error inesperado: " + e.getMessage());
            sincronizacionDAO.save(sync);
            System.err.println("✗ Error al procesar sincronización para " +
                             sync.getUsuarioCedula() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Intenta sincronizar un documento individual con el central.
     * @return true si tuvo éxito, false si falló
     */
    private boolean sincronizarDocumento(documento_clinico documento, UUID tenantId) {
        try {
            UUID historiaClinicaId = centralAPIClient.registrarDocumentoHistoriaClinica(
                tenantId.toString(),
                documento.getUsuarioSaludCedula(),
                documento.getId()
            );

            if (historiaClinicaId != null) {
                // Actualizar documento con el ID de historia clínica
                documento.setHistClinicaId(historiaClinicaId);
                documentoDAO.save(documento);
                System.out.println("  ✓ Documento " + documento.getId() + " sincronizado con historia " +
                                 historiaClinicaId);
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("  ✗ Error al sincronizar documento " + documento.getId() +
                             ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Método auxiliar para forzar un procesamiento inmediato (útil para testing/admin).
     * Puede ser invocado desde un endpoint REST de administración.
     */
    public void procesarInmediato() {
        System.out.println("Procesamiento inmediato solicitado.");
        procesarReintentos();
    }
}
