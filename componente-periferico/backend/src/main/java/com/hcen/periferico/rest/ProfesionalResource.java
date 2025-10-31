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
    public Response getAllProfesionales(
            @QueryParam("ci") Integer ci,
            @QueryParam("tipoDoc") String tipoDocumento,
            @QueryParam("search") String searchTerm,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") Integer size) {
        try {
            int pageSize = size != null && size > 0 ? Math.min(size, 200) : 10;

            if (ci != null) {
                Optional<profesional_salud> profesional = profesionalService.getProfesionalByCi(ci);
                if (profesional.isEmpty()) {
                    return Response.ok(
                        new PaginatedResponse(List.of(), 0, page, pageSize)
                    ).build();
                }
                profesional_salud_dto dto = toDTO(profesional.get());
                PaginatedResponse response = new PaginatedResponse(List.of(dto), 1, 0, 1);
                return Response.ok(response).build();
            }

            List<profesional_salud> profesionales;
            long totalCount;

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                profesionales = profesionalService.searchProfesionalesPaginated(searchTerm, page, pageSize);
                totalCount = profesionalService.countProfesionalesBySearch(searchTerm);
            } else {
                profesionales = profesionalService.getProfesionalesPaginated(page, pageSize);
                totalCount = profesionalService.countProfesionales();
            }

            List<profesional_salud_dto> dtos = profesionales.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            PaginatedResponse response = new PaginatedResponse(dtos, totalCount, page, pageSize);
            return Response.ok(response).build();
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

    @PUT
    @Path("/{ci}")
    public Response updateProfesional(@PathParam("ci") Integer ci, profesional_salud_dto dto) {
        try {
            if (dto.getCi() != null && !dto.getCi().equals(ci)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El número de documento no se puede modificar"))
                    .build();
            }
            profesional_salud profesional = profesionalService.saveProfesional(
                ci,
                dto.getNombre(),
                dto.getApellidos(),
                dto.getEspecialidad(),
                dto.getEmail()
            );
            return Response.ok(toDTO(profesional)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al actualizar profesional: " + e.getMessage()))
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

    public static class PaginatedResponse {
        private List<profesional_salud_dto> data;
        private long totalCount;
        private int currentPage;
        private int pageSize;
        private long totalPages;

        public PaginatedResponse() {}

        public PaginatedResponse(List<profesional_salud_dto> data, long totalCount, int currentPage, int pageSize) {
            this.data = data;
            this.totalCount = totalCount;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = pageSize > 0 ? (long) Math.ceil((double) totalCount / pageSize) : 0;
        }

        public List<profesional_salud_dto> getData() { return data; }
        public void setData(List<profesional_salud_dto> data) { this.data = data; }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public long getTotalPages() { return totalPages; }
        public void setTotalPages(long totalPages) { this.totalPages = totalPages; }
    }
}
