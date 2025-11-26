package com.hcen.periferico.rest;

import com.hcen.periferico.dto.documento_clinico_dto;
import com.hcen.periferico.entity.documento_clinico;
import com.hcen.periferico.service.DocumentoClinicoService;
import com.hcen.periferico.dao.DocumentoClinicoDAO;
import com.hcen.periferico.service.SincronizacionReintentosService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Resource para gestión de documentos clínicos ambulatorios.
 */
@Path("/documentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DocumentoClinicoResource {

    @EJB
    private DocumentoClinicoService documentoService;

    @EJB
    private DocumentoClinicoDAO documentoDAO;

    @EJB
    private SincronizacionReintentosService sincronizacionService;

    /**
     * Crea un nuevo documento clínico
     * POST /api/documentos?tenantId=xxx
     */
    @POST
    public Response crearDocumento(DocumentoRequest request, @QueryParam("tenantId") String tenantIdStr) {
        try {
            // Validar tenantId
            if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El parámetro tenantId es requerido"))
                        .build();
            }

            UUID tenantId = UUID.fromString(tenantIdStr);

            // Crear documento
            documento_clinico documento = documentoService.crearDocumento(
                    request.getUsuarioSaludCedula(),
                    request.getProfesionalCi(),
                    request.getCodigoMotivoConsulta(),
                    request.getDescripcionDiagnostico(),
                    request.getFechaInicioDiagnostico(),
                    request.getCodigoEstadoProblema(),
                    request.getCodigoGradoCerteza(),
                    request.getFechaProximaConsulta(),
                    request.getDescripcionProximaConsulta(),
                    request.getReferenciaAlta(),
                    tenantId
            );

            // Obtener DTO con información completa
            Optional<documento_clinico_dto> dtoOpt = documentoService.getDocumentoPorId(documento.getId(), tenantId);
            if (dtoOpt.isPresent()) {
                return Response.status(Response.Status.CREATED).entity(dtoOpt.get()).build();
            } else {
                return Response.ok(new SuccessResponse("Documento creado con ID: " + documento.getId())).build();
            }

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al crear documento: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Actualiza un documento clínico existente
     * PUT /api/documentos/{id}?tenantId=xxx
     */
    @PUT
    @Path("/{id}")
    public Response actualizarDocumento(
            @PathParam("id") String documentoIdStr,
            DocumentoRequest request,
            @QueryParam("tenantId") String tenantIdStr) {
        try {
            // Validar tenantId
            if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El parámetro tenantId es requerido"))
                        .build();
            }

            UUID documentoId = UUID.fromString(documentoIdStr);
            UUID tenantId = UUID.fromString(tenantIdStr);

            // Actualizar documento
            documento_clinico documento = documentoService.actualizarDocumento(
                    documentoId,
                    request.getCodigoMotivoConsulta(),
                    request.getDescripcionDiagnostico(),
                    request.getFechaInicioDiagnostico(),
                    request.getCodigoEstadoProblema(),
                    request.getCodigoGradoCerteza(),
                    request.getFechaProximaConsulta(),
                    request.getDescripcionProximaConsulta(),
                    request.getReferenciaAlta(),
                    tenantId
            );

            // Obtener DTO con información completa
            Optional<documento_clinico_dto> dtoOpt = documentoService.getDocumentoPorId(documento.getId(), tenantId);
            return dtoOpt.map(dto -> Response.ok(dto).build())
                    .orElse(Response.ok(new SuccessResponse("Documento actualizado")).build());

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al actualizar documento: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Obtiene un documento por ID
     * GET /api/documentos/{id}?tenantId=xxx
     */
    @GET
    @Path("/{id}")
    public Response getDocumentoPorId(
            @PathParam("id") String documentoIdStr,
            @QueryParam("tenantId") String tenantIdStr) {
        try {
            if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El parámetro tenantId es requerido"))
                        .build();
            }

            UUID documentoId = UUID.fromString(documentoIdStr);
            UUID tenantId = UUID.fromString(tenantIdStr);

            Optional<documento_clinico_dto> dtoOpt = documentoService.getDocumentoPorId(documentoId, tenantId);
            if (dtoOpt.isPresent()) {
                return Response.ok(dtoOpt.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Documento no encontrado"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al obtener documento: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Lista documentos de un paciente
     * GET /api/documentos/paciente/{cedula}?tenantId=xxx
     */
    @GET
    @Path("/paciente/{cedula}")
    public Response getDocumentosPorPaciente(
            @PathParam("cedula") String cedula,
            @QueryParam("tenantId") String tenantIdStr) {
        try {
            if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El parámetro tenantId es requerido"))
                        .build();
            }

            UUID tenantId = UUID.fromString(tenantIdStr);
            List<documento_clinico_dto> documentos = documentoService.getDocumentosPorPaciente(cedula, tenantId);
            long totalCount = documentoService.countDocumentosPorPaciente(cedula, tenantId);

            return Response.ok(new DocumentoListResponse(documentos, totalCount)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al listar documentos del paciente: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Lista documentos firmados por un profesional
     * GET /api/documentos/profesional/{ci}?tenantId=xxx
     */
    @GET
    @Path("/profesional/{ci}")
    public Response getDocumentosPorProfesional(
            @PathParam("ci") Integer profesionalCi,
            @QueryParam("tenantId") String tenantIdStr) {
        try {
            if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El parámetro tenantId es requerido"))
                        .build();
            }

            UUID tenantId = UUID.fromString(tenantIdStr);
            List<documento_clinico_dto> documentos = documentoService.getDocumentosPorProfesional(profesionalCi, tenantId);

            return Response.ok(documentos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al listar documentos del profesional: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Lista todos los documentos con paginación o por IDs específicos (batch)
     * GET /api/documentos?tenantId=xxx&page=0&size=10 (paginación)
     * GET /api/documentos?tenantId=xxx&ids=id1,id2,id3 (batch por IDs con filtro de tenant)
     * GET /api/documentos?ids=id1,id2,id3 (batch cross-tenant - sin filtro de tenant)
     */
    @GET
    public Response getDocumentosPaginados(
            @QueryParam("tenantId") String tenantIdStr,
            @QueryParam("ids") String idsStr,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") Integer size) {
        try {
            // Si se proporciona 'ids', retornar documentos por IDs (batch)
            if (idsStr != null && !idsStr.trim().isEmpty()) {
                try {
                    String[] idArray = idsStr.split(",");
                    List<UUID> documentoIds = new ArrayList<>();
                    for (String idStr : idArray) {
                        documentoIds.add(UUID.fromString(idStr.trim()));
                    }

                    List<documento_clinico_dto> documentos;

                    // Si se proporciona tenantId, filtrar por tenant (compatibilidad hacia atrás)
                    // Si NO se proporciona tenantId, buscar en todos los tenants (cross-tenant para central)
                    if (tenantIdStr != null && !tenantIdStr.trim().isEmpty()) {
                        UUID tenantId = UUID.fromString(tenantIdStr);
                        documentos = documentoService.getDocumentosPorIds(documentoIds, tenantId);
                    } else {
                        // Cross-tenant: buscar documentos en cualquier tenant
                        documentos = documentoService.getDocumentosPorIds(documentoIds);
                    }

                    return Response.ok(documentos).build();
                } catch (IllegalArgumentException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("IDs inválidos: " + e.getMessage()))
                            .build();
                }
            }

            // Para paginación, tenantId es requerido
            if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El parámetro tenantId es requerido para paginación"))
                        .build();
            }

            UUID tenantId = UUID.fromString(tenantIdStr);

            // Usar paginación normal
            int pageSize = size != null && size > 0 ? Math.min(size, 200) : 10;

            List<documento_clinico_dto> documentos = documentoService.getDocumentosPaginados(tenantId, page, pageSize);
            long totalCount = documentoService.countDocumentos(tenantId);

            PaginatedResponse response = new PaginatedResponse(documentos, totalCount, page, pageSize);
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al listar documentos: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Elimina un documento
     * DELETE /api/documentos/{id}?tenantId=xxx
     */
    @DELETE
    @Path("/{id}")
    public Response eliminarDocumento(
            @PathParam("id") String documentoIdStr,
            @QueryParam("tenantId") String tenantIdStr) {
        try {
            if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El parámetro tenantId es requerido"))
                        .build();
            }

            UUID documentoId = UUID.fromString(documentoIdStr);
            UUID tenantId = UUID.fromString(tenantIdStr);

            boolean eliminado = documentoService.eliminarDocumento(documentoId, tenantId);
            if (eliminado) {
                return Response.ok(new SuccessResponse("Documento eliminado exitosamente")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Documento no encontrado"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al eliminar documento: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Obtiene los catálogos de codigueras
     * GET /api/documentos/catalogos/motivos
     * GET /api/documentos/catalogos/estados
     * GET /api/documentos/catalogos/grados-certeza
     */
    @GET
    @Path("/catalogos/motivos")
    public Response getMotivosConsulta() {
        try {
            Map<String, String> motivos = documentoDAO.getAllMotivosConsulta();
            return Response.ok(motivos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al obtener catálogo: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Busca motivos de consulta por término (para autocompletado)
     * GET /api/documentos/catalogos/motivos/buscar?termino=dolor
     */
    @GET
    @Path("/catalogos/motivos/buscar")
    public Response buscarMotivosConsulta(@QueryParam("termino") String termino) {
        try {
            if (termino == null || termino.trim().isEmpty()) {
                // Si no hay término, devolver lista vacía o los primeros N
                return Response.ok(new LinkedHashMap<String, String>()).build();
            }

            Map<String, String> motivos = documentoDAO.buscarMotivosConsulta(termino.trim());
            return Response.ok(motivos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al buscar motivos: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/catalogos/estados")
    public Response getEstadosProblema() {
        try {
            Map<String, String> estados = documentoDAO.getAllEstadosProblema();
            return Response.ok(estados).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al obtener catálogo: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/catalogos/grados-certeza")
    public Response getGradosCerteza() {
        try {
            Map<String, String> grados = documentoDAO.getAllGradosCerteza();
            return Response.ok(grados).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al obtener catálogo: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Fuerza la sincronización inmediata de documentos pendientes con el componente central
     * POST /api/documentos/sincronizar-pendientes
     *
     * NOTA: Este es un endpoint provisional para testing/debugging.
     * En producción debería estar protegido con autenticación de admin.
     */
    @POST
    @Path("/sincronizar-pendientes")
    public Response sincronizarPendientes() {
        try {
            System.out.println("=== Sincronización manual solicitada desde frontend ===");
            int reenviados = sincronizacionService.procesarInmediato();
            return Response.ok(new SuccessResponse(
                    "Reintentos disparados para " + reenviados + " documentos pendientes")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al sincronizar: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Valida si un profesional tiene permiso para acceder a uno o múltiples documentos
     * Llama al componente central para verificar las políticas de acceso
     */
    @POST
    @Path("/validar-acceso")
    public Response validarAccesoDocumento(
            @QueryParam("tenantId") String tenantIdStr,
            ValidarAccesoRequest request) {
        try {
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El tenantId es requerido"))
                        .build();
            }
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Los datos de la solicitud son requeridos"))
                        .build();
            }
            if (request.getDocumentoIds() == null || request.getDocumentoIds().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("La lista de IDs de documentos es requerida"))
                        .build();
            }
            if (request.getCiProfesional() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El CI del profesional es requerido"))
                        .build();
            }

            UUID tenantId = UUID.fromString(tenantIdStr);

            // Convertir strings a UUIDs
            List<UUID> documentoIds = request.getDocumentoIds().stream()
                    .map(UUID::fromString)
                    .collect(java.util.stream.Collectors.toList());

            // Validar acceso batch
            Map<UUID, Boolean> permisos = documentoService.validarAccesoDocumentos(
                    documentoIds,
                    request.getCiProfesional(),
                    tenantId,
                    request.getEspecialidad()
            );

            // Convertir UUIDs a Strings para response
            Map<String, Boolean> permisosStr = new java.util.HashMap<>();
            permisos.forEach((uuid, hasPermission) -> permisosStr.put(uuid.toString(), hasPermission));

            return Response.ok(new ValidarAccesoResponse(permisosStr)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Parámetros inválidos: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            System.err.println("Error al validar acceso a documentos: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al validar acceso: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Solicita acceso a un documento enviando notificación al paciente
     * Solo permite una solicitud por minuto por profesional-documento
     */
    @POST
    @Path("/{id}/solicitar-acceso")
    public Response solicitarAccesoDocumento(
            @PathParam("id") String documentoIdStr,
            @QueryParam("tenantId") String tenantIdStr,
            SolicitarAccesoRequest request) {
        try {
            if (documentoIdStr == null || documentoIdStr.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El ID del documento es requerido"))
                        .build();
            }
            if (tenantIdStr == null || tenantIdStr.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El tenantId es requerido"))
                        .build();
            }
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Los datos de la solicitud son requeridos"))
                        .build();
            }
            if (request.getCiProfesional() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El CI del profesional es requerido"))
                        .build();
            }
            if (request.getNombreProfesional() == null || request.getNombreProfesional().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("El nombre del profesional es requerido"))
                        .build();
            }

            UUID documentoId = UUID.fromString(documentoIdStr);
            UUID tenantId = UUID.fromString(tenantIdStr);

            DocumentoClinicoService.ResultadoSolicitudAcceso resultado = documentoService.solicitarAccesoDocumento(
                    documentoId,
                    request.getCiProfesional(),
                    request.getNombreProfesional(),
                    request.getEspecialidad(),
                    tenantId
            );

            // Retornar siempre 200 OK con el mensaje apropiado
            // No se considera error si ya existe una solicitud reciente
            return Response.ok(new SolicitarAccesoResponse(
                    resultado.isExitoso(),
                    resultado.getMensaje()))
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Parámetros inválidos: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            System.err.println("Error al solicitar acceso a documento: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error al enviar solicitud: " + e.getMessage()))
                    .build();
        }
    }

    // ============ CLASES AUXILIARES ============

    public static class DocumentoRequest {
        private String usuarioSaludCedula;
        private Integer profesionalCi;
        private String codigoMotivoConsulta;
        private String descripcionDiagnostico;
        private LocalDate fechaInicioDiagnostico;
        private String codigoEstadoProblema;
        private String codigoGradoCerteza;
        private LocalDate fechaProximaConsulta;
        private String descripcionProximaConsulta;
        private String referenciaAlta;

        // Getters y Setters
        public String getUsuarioSaludCedula() { return usuarioSaludCedula; }
        public void setUsuarioSaludCedula(String usuarioSaludCedula) { this.usuarioSaludCedula = usuarioSaludCedula; }
        public Integer getProfesionalCi() { return profesionalCi; }
        public void setProfesionalCi(Integer profesionalCi) { this.profesionalCi = profesionalCi; }
        public String getCodigoMotivoConsulta() { return codigoMotivoConsulta; }
        public void setCodigoMotivoConsulta(String codigoMotivoConsulta) { this.codigoMotivoConsulta = codigoMotivoConsulta; }
        public String getDescripcionDiagnostico() { return descripcionDiagnostico; }
        public void setDescripcionDiagnostico(String descripcionDiagnostico) { this.descripcionDiagnostico = descripcionDiagnostico; }
        public LocalDate getFechaInicioDiagnostico() { return fechaInicioDiagnostico; }
        public void setFechaInicioDiagnostico(LocalDate fechaInicioDiagnostico) { this.fechaInicioDiagnostico = fechaInicioDiagnostico; }
        public String getCodigoEstadoProblema() { return codigoEstadoProblema; }
        public void setCodigoEstadoProblema(String codigoEstadoProblema) { this.codigoEstadoProblema = codigoEstadoProblema; }
        public String getCodigoGradoCerteza() { return codigoGradoCerteza; }
        public void setCodigoGradoCerteza(String codigoGradoCerteza) { this.codigoGradoCerteza = codigoGradoCerteza; }
        public LocalDate getFechaProximaConsulta() { return fechaProximaConsulta; }
        public void setFechaProximaConsulta(LocalDate fechaProximaConsulta) { this.fechaProximaConsulta = fechaProximaConsulta; }
        public String getDescripcionProximaConsulta() { return descripcionProximaConsulta; }
        public void setDescripcionProximaConsulta(String descripcionProximaConsulta) { this.descripcionProximaConsulta = descripcionProximaConsulta; }
        public String getReferenciaAlta() { return referenciaAlta; }
        public void setReferenciaAlta(String referenciaAlta) { this.referenciaAlta = referenciaAlta; }
    }

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

    public static class DocumentoListResponse {
        private List<documento_clinico_dto> data;
        private long totalCount;

        public DocumentoListResponse() {}
        public DocumentoListResponse(List<documento_clinico_dto> data, long totalCount) {
            this.data = data;
            this.totalCount = totalCount;
        }

        public List<documento_clinico_dto> getData() { return data; }
        public void setData(List<documento_clinico_dto> data) { this.data = data; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    }

    public static class PaginatedResponse {
        private List<documento_clinico_dto> data;
        private long totalCount;
        private int currentPage;
        private int pageSize;
        private long totalPages;

        public PaginatedResponse() {}

        public PaginatedResponse(List<documento_clinico_dto> data, long totalCount, int currentPage, int pageSize) {
            this.data = data;
            this.totalCount = totalCount;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = pageSize > 0 ? (long) Math.ceil((double) totalCount / pageSize) : 0;
        }

        public List<documento_clinico_dto> getData() { return data; }
        public void setData(List<documento_clinico_dto> data) { this.data = data; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public long getTotalPages() { return totalPages; }
        public void setTotalPages(long totalPages) { this.totalPages = totalPages; }
    }

    // Request para validar acceso
    public static class ValidarAccesoRequest {
        private List<String> documentoIds;
        private Integer ciProfesional;
        private String especialidad;

        public List<String> getDocumentoIds() { return documentoIds; }
        public void setDocumentoIds(List<String> documentoIds) { this.documentoIds = documentoIds; }
        public Integer getCiProfesional() { return ciProfesional; }
        public void setCiProfesional(Integer ciProfesional) { this.ciProfesional = ciProfesional; }
        public String getEspecialidad() { return especialidad; }
        public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    }

    // Response de validar acceso
    public static class ValidarAccesoResponse {
        private Map<String, Boolean> permisos;

        public ValidarAccesoResponse() {}
        public ValidarAccesoResponse(Map<String, Boolean> permisos) {
            this.permisos = permisos;
        }

        public Map<String, Boolean> getPermisos() { return permisos; }
        public void setPermisos(Map<String, Boolean> permisos) { this.permisos = permisos; }
    }

    // Request para solicitar acceso
    public static class SolicitarAccesoRequest {
        private Integer ciProfesional;
        private String nombreProfesional;
        private String especialidad;

        public Integer getCiProfesional() { return ciProfesional; }
        public void setCiProfesional(Integer ciProfesional) { this.ciProfesional = ciProfesional; }
        public String getNombreProfesional() { return nombreProfesional; }
        public void setNombreProfesional(String nombreProfesional) { this.nombreProfesional = nombreProfesional; }
        public String getEspecialidad() { return especialidad; }
        public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    }

    // Response para solicitar acceso
    public static class SolicitarAccesoResponse {
        private boolean exitoso;
        private String mensaje;

        public SolicitarAccesoResponse() {}
        public SolicitarAccesoResponse(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }

        public boolean isExitoso() { return exitoso; }
        public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    }
}
