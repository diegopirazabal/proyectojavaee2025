package com.hcen.periferico.rest;

import com.hcen.periferico.dao.EspecialidadDAO;
import com.hcen.periferico.dto.especialidad_dto;
import com.hcen.periferico.entity.Especialidad;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST Resource para gestión de especialidades médicas.
 * Proporciona endpoints para listar especialidades disponibles.
 */
@Path("/especialidades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EspecialidadResource {

    private static final Logger LOGGER = Logger.getLogger(EspecialidadResource.class.getName());

    @EJB
    private EspecialidadDAO especialidadDAO;

    /**
     * GET /api/especialidades
     * Obtiene todas las especialidades ordenadas alfabéticamente
     */
    @GET
    public Response getAllEspecialidades() {
        try {
            LOGGER.info("[EspecialidadResource] getAllEspecialidades");

            List<Especialidad> especialidades = especialidadDAO.findAll();
            List<especialidad_dto> dtos = especialidades.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            LOGGER.info(() -> "[EspecialidadResource] Devolviendo " + dtos.size() + " especialidades");
            return Response.ok(dtos).build();

        } catch (Exception e) {
            LOGGER.severe("[EspecialidadResource] Error al obtener especialidades: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener especialidades"))
                .build();
        }
    }

    /**
     * GET /api/especialidades/{id}
     * Obtiene una especialidad por ID
     */
    @GET
    @Path("/{id}")
    public Response getEspecialidadById(@PathParam("id") String idStr) {
        try {
            UUID id = UUID.fromString(idStr);
            LOGGER.info(() -> "[EspecialidadResource] getEspecialidadById: " + id);

            return especialidadDAO.findById(id)
                .map(this::toDTO)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Especialidad no encontrada"))
                    .build());

        } catch (IllegalArgumentException e) {
            LOGGER.warning("[EspecialidadResource] ID inválido: " + idStr);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("ID de especialidad inválido"))
                .build();
        } catch (Exception e) {
            LOGGER.severe("[EspecialidadResource] Error al obtener especialidad: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener especialidad"))
                .build();
        }
    }

    /**
     * GET /api/especialidades/search?q=termino
     * Busca especialidades por nombre parcial
     */
    @GET
    @Path("/search")
    public Response searchEspecialidades(@QueryParam("q") String searchTerm) {
        try {
            LOGGER.info(() -> "[EspecialidadResource] searchEspecialidades: q=" + searchTerm);

            List<Especialidad> especialidades = especialidadDAO.findByNombreContaining(searchTerm);
            List<especialidad_dto> dtos = especialidades.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            LOGGER.info(() -> "[EspecialidadResource] Encontradas " + dtos.size() + " especialidades");
            return Response.ok(dtos).build();

        } catch (Exception e) {
            LOGGER.severe("[EspecialidadResource] Error al buscar especialidades: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al buscar especialidades"))
                .build();
        }
    }

    /**
     * Convierte entidad Especialidad a DTO
     */
    private especialidad_dto toDTO(Especialidad entity) {
        return new especialidad_dto(
            entity.getId(),
            entity.getNombre()
        );
    }

    /**
     * Clase interna para respuestas de error
     */
    public static class ErrorResponse {
        private String error;

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
