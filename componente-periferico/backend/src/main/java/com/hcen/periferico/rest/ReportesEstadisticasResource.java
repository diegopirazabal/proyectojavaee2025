package com.hcen.periferico.rest;

import com.hcen.periferico.service.ReportesEstadisticasService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
