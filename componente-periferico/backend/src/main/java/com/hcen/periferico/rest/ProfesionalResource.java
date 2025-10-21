package com.hcen.periferico.rest;

import com.hcen.core.domain.profesional_salud;
import com.hcen.periferico.dto.profesional_salud_dto;
import com.hcen.periferico.service.ProfesionalService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/profesionales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfesionalResource {

    @EJB
    private ProfesionalService profesionalService;

    @GET
    public Response getAllProfesionales(@QueryParam("search") String searchTerm) {
        try {
            List<profesional_salud> profesionales;
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                profesionales = profesionalService.searchProfesionales(searchTerm);
            } else {
                profesionales = profesionalService.getAllProfesionales();
            }

            List<profesional_salud_dto> dtos = profesionales.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            return Response.ok(dtos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al listar profesionales: " + e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{ci}")
    public Response getProfesionalByCi(@PathParam("ci") Integer ci) {
        try {
            Optional<profesional_salud> profesional = profesionalService.getProfesionalByCi(ci);
            if (profesional.isPresent()) {
                profesional_salud_dto dto = toDTO(profesional.get());
                return Response.ok(dto).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Profesional no encontrado"))
                    .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener profesional: " + e.getMessage()))
                .build();
        }
    }

    @POST
    public Response saveProfesional(profesional_salud_dto dto) {
        try {
            profesional_salud profesional = profesionalService.saveProfesional(
                dto.getCi(),
                dto.getNombre(),
                dto.getApellidos(),
                dto.getEspecialidad(),
                dto.getEmail()
            );
            profesional_salud_dto resultDto = toDTO(profesional);
            return Response.ok(resultDto).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al guardar profesional: " + e.getMessage()))
                .build();
        }
    }

    @DELETE
    @Path("/{ci}")
    public Response deleteProfesional(@PathParam("ci") Integer ci) {
        try {
            profesionalService.deleteProfesional(ci);
            return Response.ok(new SuccessResponse("Profesional eliminado exitosamente")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al eliminar profesional: " + e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/especialidad/{especialidad}")
    public Response getProfesionalesByEspecialidad(@PathParam("especialidad") String especialidad) {
        try {
            List<profesional_salud> profesionales = profesionalService.getProfesionalesByEspecialidad(especialidad);
            List<profesional_salud_dto> dtos = profesionales.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
            return Response.ok(dtos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al buscar por especialidad: " + e.getMessage()))
                .build();
        }
    }

    private profesional_salud_dto toDTO(profesional_salud entity) {
        return new profesional_salud_dto(
            entity.getCi(),
            entity.getNombre(),
            entity.getApellidos(),
            entity.getEspecialidad(),
            entity.getEmail()
        );
    }

    // Clases auxiliares
    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}
        public ErrorResponse(String error) { this.error = error; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class SuccessResponse {
        private String message;

        public SuccessResponse() {}
        public SuccessResponse(String message) { this.message = message; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}