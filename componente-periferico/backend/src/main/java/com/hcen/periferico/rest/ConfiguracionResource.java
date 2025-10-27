package com.hcen.periferico.rest;

import com.hcen.core.domain.configuracion_clinica;
import com.hcen.periferico.dto.configuracion_clinica_dto;
import com.hcen.periferico.service.ConfiguracionService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/configuracion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfiguracionResource {

    @EJB
    private ConfiguracionService configuracionService;

    @GET
    @Path("/{tenantId}")
    public Response getConfiguracion(@PathParam("tenantId") String tenantId) {
        try {
            configuracion_clinica config = configuracionService.getConfiguracion(tenantId);
            configuracion_clinica_dto dto = toDTO(config);
            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener configuración: " + e.getMessage()))
                .build();
        }
    }

    @PUT
    @Path("/{tenantId}/lookfeel")
    public Response updateLookAndFeel(@PathParam("tenantId") String tenantId,
                                      LookFeelRequest request) {
        try {
            configuracion_clinica config = configuracionService.actualizarLookAndFeel(
                tenantId,
                request.getColorPrimario(),
                request.getColorSecundario(),
                request.getLogoUrl(),
                request.getNombreSistema(),
                request.getTema()
            );
            configuracion_clinica_dto dto = toDTO(config);
            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al actualizar look&feel: " + e.getMessage()))
                .build();
        }
    }

    @PUT
    @Path("/{tenantId}/nodo")
    public Response toggleNodoPeriferico(@PathParam("tenantId") String tenantId,
                                         NodoRequest request) {
        try {
            configuracion_clinica config = configuracionService.toggleNodoPeriferico(
                tenantId,
                request.getHabilitado()
            );
            configuracion_clinica_dto dto = toDTO(config);
            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al cambiar nodo: " + e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{tenantId}/reset")
    public Response resetToDefault(@PathParam("tenantId") String tenantId) {
        try {
            configuracion_clinica config = configuracionService.resetToDefault(tenantId);
            configuracion_clinica_dto dto = toDTO(config);
            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al resetear configuración: " + e.getMessage()))
                .build();
        }
    }

    private configuracion_clinica_dto toDTO(configuracion_clinica entity) {
        configuracion_clinica_dto dto = new configuracion_clinica_dto();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getTenantId());
        dto.setColorPrimario(entity.getColorPrimario());
        dto.setColorSecundario(entity.getColorSecundario());
        dto.setLogoUrl(entity.getLogoUrl());
        dto.setNombreSistema(entity.getNombreSistema());
        dto.setTema(entity.getTema());
        dto.setNodoPerifericoHabilitado(entity.getNodoPerifericoHabilitado());
        return dto;
    }

    // Clases auxiliares
    public static class LookFeelRequest {
        private String colorPrimario;
        private String colorSecundario;
        private String logoUrl;
        private String nombreSistema;
        private String tema;

        public String getColorPrimario() { return colorPrimario; }
        public void setColorPrimario(String colorPrimario) { this.colorPrimario = colorPrimario; }
        public String getColorSecundario() { return colorSecundario; }
        public void setColorSecundario(String colorSecundario) { this.colorSecundario = colorSecundario; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
        public String getNombreSistema() { return nombreSistema; }
        public void setNombreSistema(String nombreSistema) { this.nombreSistema = nombreSistema; }
        public String getTema() { return tema; }
        public void setTema(String tema) { this.tema = tema; }
    }

    public static class NodoRequest {
        private Boolean habilitado;

        public Boolean getHabilitado() { return habilitado; }
        public void setHabilitado(Boolean habilitado) { this.habilitado = habilitado; }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}
        public ErrorResponse(String error) { this.error = error; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}