package com.hcen.periferico.rest;

import com.hcen.periferico.dao.SolicitudAccesoDocumentoDAO;
import com.hcen.periferico.dto.ActualizarSolicitudRequest;
import com.hcen.periferico.entity.solicitud_acceso_documento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Resource para actualizar estado de solicitudes de acceso a documentos.
 * Usado por el componente central para notificar cuando un paciente aprueba/rechaza una solicitud.
 */
@Stateless
@Path("/api/solicitudes-acceso")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SolicitudAccesoResource {

    private static final Logger LOGGER = Logger.getLogger(SolicitudAccesoResource.class.getName());

    @EJB
    private SolicitudAccesoDocumentoDAO solicitudDAO;

    /**
     * Marca una solicitud de acceso como APROBADA
     * Llamado por el componente central cuando el paciente otorga permiso
     *
     * @param solicitudIdStr UUID de la solicitud (path parameter)
     * @param request Request con tenantId para validación
     * @return 200 OK si se actualizó correctamente, códigos de error apropiados en caso contrario
     */
    @PUT
    @Path("/{solicitudId}/aprobar")
    public Response aprobarSolicitud(
            @PathParam("solicitudId") String solicitudIdStr,
            ActualizarSolicitudRequest request) {

        LOGGER.info(String.format("Recibida solicitud para aprobar solicitud: %s (tenantId: %s)",
                solicitudIdStr, request != null ? request.getTenantId() : "null"));

        try {
            // 1. Validar que el request contenga tenantId
            if (request == null || request.getTenantId() == null || request.getTenantId().isBlank()) {
                return buildErrorResponse(Response.Status.BAD_REQUEST, "El tenantId es requerido");
            }

            // 2. Validar formato del UUID
            UUID solicitudId;
            try {
                solicitudId = UUID.fromString(solicitudIdStr);
            } catch (IllegalArgumentException e) {
                return buildErrorResponse(Response.Status.BAD_REQUEST,
                        "El solicitudId no es un UUID válido: " + solicitudIdStr);
            }

            UUID tenantId = UUID.fromString(request.getTenantId());

            // 3. Buscar la solicitud en la base de datos
            Optional<solicitud_acceso_documento> solicitudOpt = solicitudDAO.findById(solicitudId);
            if (solicitudOpt.isEmpty()) {
                return buildErrorResponse(Response.Status.NOT_FOUND,
                        "No se encontró la solicitud con ID " + solicitudId);
            }

            solicitud_acceso_documento solicitud = solicitudOpt.get();

            // 4. Validación de seguridad multi-tenant
            if (!solicitud.getTenantId().equals(tenantId)) {
                LOGGER.warning(String.format(
                        "Intento de acceso cross-tenant detectado: solicitudId=%s, solicitudTenantId=%s, requestTenantId=%s",
                        solicitudId, solicitud.getTenantId(), tenantId));

                return buildErrorResponse(Response.Status.FORBIDDEN,
                        "No tiene permisos para actualizar esta solicitud (tenant mismatch)");
            }

            // 5. Validar estado actual (idempotencia)
            if (solicitud.getEstado() != solicitud_acceso_documento.EstadoSolicitud.PENDIENTE) {
                // Si ya está aprobada, retornar OK (idempotencia)
                if (solicitud.getEstado() == solicitud_acceso_documento.EstadoSolicitud.APROBADA) {
                    LOGGER.info(String.format("Solicitud %s ya estaba aprobada, ignorando duplicado", solicitudId));
                    return buildSuccessResponse("La solicitud ya fue aprobada previamente");
                }

                // Si está rechazada, retornar conflicto
                return buildErrorResponse(Response.Status.CONFLICT,
                        "La solicitud ya fue procesada con estado: " + solicitud.getEstado());
            }

            // 6. Aprobar la solicitud
            solicitudDAO.aprobarSolicitud(solicitudId);

            LOGGER.info(String.format("Solicitud %s aprobada exitosamente (documento=%s, profesional=%d, paciente=%s)",
                    solicitudId, solicitud.getDocumentoId(), solicitud.getProfesionalCi(), solicitud.getCedulaPaciente()));

            return buildSuccessResponse("Solicitud aprobada exitosamente");

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Error de validación al aprobar solicitud: " + solicitudIdStr, e);
            return buildErrorResponse(Response.Status.BAD_REQUEST, "Error de validación: " + e.getMessage());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al aprobar solicitud: " + solicitudIdStr, e);
            return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "Error interno al aprobar la solicitud");
        }
    }

    /**
     * Marca una solicitud de acceso como RECHAZADA
     * Llamado por el componente central cuando el paciente deniega permiso
     *
     * @param solicitudIdStr UUID de la solicitud (path parameter)
     * @param request Request con tenantId para validación
     * @return 200 OK si se actualizó correctamente, códigos de error apropiados en caso contrario
     */
    @PUT
    @Path("/{solicitudId}/rechazar")
    public Response rechazarSolicitud(
            @PathParam("solicitudId") String solicitudIdStr,
            ActualizarSolicitudRequest request) {

        LOGGER.info(String.format("Recibida solicitud para rechazar solicitud: %s (tenantId: %s)",
                solicitudIdStr, request != null ? request.getTenantId() : "null"));

        try {
            // 1. Validar que el request contenga tenantId
            if (request == null || request.getTenantId() == null || request.getTenantId().isBlank()) {
                return buildErrorResponse(Response.Status.BAD_REQUEST, "El tenantId es requerido");
            }

            // 2. Validar formato del UUID
            UUID solicitudId;
            try {
                solicitudId = UUID.fromString(solicitudIdStr);
            } catch (IllegalArgumentException e) {
                return buildErrorResponse(Response.Status.BAD_REQUEST,
                        "El solicitudId no es un UUID válido: " + solicitudIdStr);
            }

            UUID tenantId = UUID.fromString(request.getTenantId());

            // 3. Buscar la solicitud en la base de datos
            Optional<solicitud_acceso_documento> solicitudOpt = solicitudDAO.findById(solicitudId);
            if (solicitudOpt.isEmpty()) {
                return buildErrorResponse(Response.Status.NOT_FOUND,
                        "No se encontró la solicitud con ID " + solicitudId);
            }

            solicitud_acceso_documento solicitud = solicitudOpt.get();

            // 4. Validación de seguridad multi-tenant
            if (!solicitud.getTenantId().equals(tenantId)) {
                LOGGER.warning(String.format(
                        "Intento de acceso cross-tenant detectado: solicitudId=%s, solicitudTenantId=%s, requestTenantId=%s",
                        solicitudId, solicitud.getTenantId(), tenantId));

                return buildErrorResponse(Response.Status.FORBIDDEN,
                        "No tiene permisos para actualizar esta solicitud (tenant mismatch)");
            }

            // 5. Validar estado actual (idempotencia)
            if (solicitud.getEstado() != solicitud_acceso_documento.EstadoSolicitud.PENDIENTE) {
                // Si ya está rechazada, retornar OK (idempotencia)
                if (solicitud.getEstado() == solicitud_acceso_documento.EstadoSolicitud.RECHAZADA) {
                    LOGGER.info(String.format("Solicitud %s ya estaba rechazada, ignorando duplicado", solicitudId));
                    return buildSuccessResponse("La solicitud ya fue rechazada previamente");
                }

                // Si está aprobada, retornar conflicto
                return buildErrorResponse(Response.Status.CONFLICT,
                        "La solicitud ya fue procesada con estado: " + solicitud.getEstado());
            }

            // 6. Rechazar la solicitud
            solicitudDAO.rechazarSolicitud(solicitudId);

            LOGGER.info(String.format("Solicitud %s rechazada exitosamente (documento=%s, profesional=%d, paciente=%s)",
                    solicitudId, solicitud.getDocumentoId(), solicitud.getProfesionalCi(), solicitud.getCedulaPaciente()));

            return buildSuccessResponse("Solicitud rechazada exitosamente");

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Error de validación al rechazar solicitud: " + solicitudIdStr, e);
            return buildErrorResponse(Response.Status.BAD_REQUEST, "Error de validación: " + e.getMessage());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al rechazar solicitud: " + solicitudIdStr, e);
            return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "Error interno al rechazar la solicitud");
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    /**
     * Construye una respuesta de éxito con formato JSON estándar
     */
    private Response buildSuccessResponse(String mensaje) {
        JsonObject json = Json.createObjectBuilder()
                .add("status", "OK")
                .add("message", mensaje)
                .build();

        return Response.ok(json).build();
    }

    /**
     * Construye una respuesta de error con formato JSON estándar
     */
    private Response buildErrorResponse(Response.Status status, String mensaje) {
        JsonObject json = Json.createObjectBuilder()
                .add("status", "ERROR")
                .add("message", mensaje)
                .build();

        return Response.status(status).entity(json).build();
    }
}
