package com.hcen.periferico.rest;

import com.hcen.periferico.entity.administrador_clinica;
import com.hcen.periferico.dto.administrador_clinica_dto;
import com.hcen.periferico.service.AdministradorClinicaService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/administradores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdministradorClinicaResource {

    @EJB
    private AdministradorClinicaService adminService;

    @GET
    public Response listarAdministradores(@QueryParam("tenantId") String tenantIdString,
                                          @QueryParam("search") String searchTerm,
                                          @QueryParam("page") @DefaultValue("0") int page,
                                          @QueryParam("size") Integer size) {
        try {
            UUID tenantId = parseUuidOrNull(tenantIdString);

            List<administrador_clinica> administradores = adminService.listarAdministradores(tenantId, searchTerm, page, size);
            List<administrador_clinica_dto> dtos = administradores.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            long total = adminService.contarAdministradores(tenantId, searchTerm);
            int resolvedSize = size != null && size > 0 ? Math.min(size, 200) : 20;

            PaginatedResponse response = new PaginatedResponse(dtos, total, page, resolvedSize);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al listar administradores: " + e.getMessage()))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response obtenerAdministrador(@PathParam("id") String id) {
        try {
            UUID adminId = UUID.fromString(id);
            administrador_clinica admin = adminService.obtenerPorId(adminId)
                .orElseThrow(() -> new NotFoundException("Administrador no encontrado"));
            return Response.ok(toDTO(admin)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Identificador inválido: " + e.getMessage()))
                .build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al obtener administrador: " + e.getMessage()))
                .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response actualizarAdministrador(@PathParam("id") String id, ActualizarAdministradorRequest request) {
        try {
            UUID adminId = UUID.fromString(id);
            UUID tenantId = request.getTenantId() != null ? UUID.fromString(request.getTenantId()) : null;

            administrador_clinica actualizado = adminService.actualizarAdministrador(
                adminId,
                request.getUsername(),
                request.getNombre(),
                request.getApellidos(),
                tenantId
            );

            return Response.ok(toDTO(actualizado)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error al actualizar administrador: " + e.getMessage()))
                .build();
        }
    }

    private administrador_clinica_dto toDTO(administrador_clinica entity) {
        return new administrador_clinica_dto(
            entity.getId(),
            entity.getUsername(),
            entity.getNombre(),
            entity.getApellidos(),
            entity.getTenantId()
        );
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return UUID.fromString(value.trim());
    }

    public static class ActualizarAdministradorRequest {
        private String username;
        private String nombre;
        private String apellidos;
        private String tenantId;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getApellidos() {
            return apellidos;
        }

        public void setApellidos(String apellidos) {
            this.apellidos = apellidos;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }

    public static class PaginatedResponse {
        private List<administrador_clinica_dto> data;
        private long totalCount;
        private int currentPage;
        private int pageSize;
        private long totalPages;

        public PaginatedResponse() {
        }

        public PaginatedResponse(List<administrador_clinica_dto> data, long totalCount, int currentPage, int pageSize) {
            this.data = data;
            this.totalCount = totalCount;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = pageSize > 0 ? (long) Math.ceil((double) totalCount / pageSize) : 0;
        }

        public List<administrador_clinica_dto> getData() {
            return data;
        }

        public void setData(List<administrador_clinica_dto> data) {
            this.data = data;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public long getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(long totalPages) {
            this.totalPages = totalPages;
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
