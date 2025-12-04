package com.hcen.periferico.rest;

import com.hcen.periferico.service.ReportesEstadisticasService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Expone métricas consolidadas para el portal administrativo central.
 */
@Path("/reportes")
@Produces(MediaType.APPLICATION_JSON)
public class ReportesEstadisticasResource {

    private static final Logger LOGGER = Logger.getLogger(ReportesEstadisticasResource.class.getName());

    @EJB
    private ReportesEstadisticasService estadisticasService;

    @GET
    @Path("/estadisticas")
    public Response obtenerEstadisticas() {
        try {
            return Response.ok(estadisticasService.generarEstadisticas()).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar estadísticas de reportes", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("No se pudieron generar las estadísticas en este momento"))
                .build();
        }
    }

    /**
     * Obtiene estadísticas de reportes con paginación de clínicas (lazy loading).
     * Los totales se calculan siempre, solo se paginan las clínicas.
     *
     * @param page Número de página (0-indexed), default 0
     * @param size Tamaño de página (1-100), default 10
     * @return Response con estadísticas paginadas
     */
    @GET
    @Path("/estadisticas/paginado")
    public Response obtenerEstadisticasPaginadas(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        try {
            // Validar y limitar parámetros
            if (size <= 0 || size > 100) {
                size = 10; // Limitar a máximo 100 por página
            }
            if (page < 0) {
                page = 0;
            }

            return Response.ok(estadisticasService.generarEstadisticasPaginadas(page, size)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar estadísticas paginadas", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("No se pudieron generar las estadísticas paginadas"))
                .build();
        }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
