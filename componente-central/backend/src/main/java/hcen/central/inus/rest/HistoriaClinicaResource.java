package hcen.central.inus.rest;

import hcen.central.inus.dto.HistoriaClinicaDocumentoDetalleResponse;
import hcen.central.inus.dto.HistoriaClinicaIdResponse;
import hcen.central.inus.service.HistoriaClinicaService;
import hcen.central.notifications.dto.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/historia-clinica")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HistoriaClinicaResource {

    private static final Logger LOGGER = Logger.getLogger(HistoriaClinicaResource.class.getName());

    @EJB
    private HistoriaClinicaService historiaClinicaService;

    @POST
    @Path("/documentos")
    public Response registrarDocumento(RegistrarDocumentoRequest request) {
        try {
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El cuerpo de la petición es requerido"))
                    .build();
            }
            if (request.getDocumentoId() == null || request.getDocumentoId().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("documentoId es requerido"))
                    .build();
            }
            if (request.getCedula() == null || request.getCedula().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("La cédula del paciente es requerida"))
                    .build();
            }
            if (request.getTenantId() == null || request.getTenantId().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("tenantId es requerido"))
                    .build();
            }

            UUID documentoUuid = UUID.fromString(request.getDocumentoId());
            UUID tenantUuid = UUID.fromString(request.getTenantId());

            UUID historiaId = historiaClinicaService.registrarDocumento(
                request.getCedula().trim(),
                tenantUuid,
                documentoUuid
            );

            RegistrarDocumentoResponse response = new RegistrarDocumentoResponse(historiaId.toString(), documentoUuid.toString());
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar documento en historia clínica", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error interno al registrar el documento: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Obtiene el ID de la historia clínica de un paciente por su cédula
     */
    @GET
    @Path("/by-cedula/{cedula}")
    public Response obtenerHistoriaIdPorCedula(@PathParam("cedula") String cedula) {
        try {
            if (cedula == null || cedula.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("La cédula es requerida"))
                    .build();
            }

            Optional<HistoriaClinicaIdResponse> historiaIdOpt = historiaClinicaService.obtenerHistoriaIdPorCedula(cedula);

            if (historiaIdOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("No existe historia clínica para la cédula " + cedula))
                    .build();
            }

            return Response.ok(ApiResponse.success(historiaIdOpt.get())).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ID de historia clínica", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al obtener el ID de historia clínica"))
                .build();
        }
    }

    /**
     * Obtiene los documentos de la historia clínica del usuario autenticado.
     * La cédula se obtiene del JWT (claim docNumber), garantizando que el usuario
     * solo pueda consultar su propia historia clínica.
     * 
     * SEGURIDAD: Requiere autenticación JWT válida.
     */
    @GET
    @Path("/mis-documentos")
    public Response obtenerMisDocumentos(@Context HttpServletRequest request) {
        try {
            // Extraer cédula del JWT (seteada por JWTAuthenticationFilter)
            Claims claims = (Claims) request.getAttribute("jwtClaims");
            
            if (claims == null) {
                LOGGER.warning("No se encontraron claims JWT en el request");
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Token JWT no válido o no presente"))
                    .build();
            }
            
            // Obtener docNumber del JWT
            String cedula = claims.get("docNumber", String.class);
            
            if (cedula == null || cedula.isBlank()) {
                LOGGER.warning("JWT no contiene docNumber");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("El token no contiene información de documento"))
                    .build();
            }
            
            LOGGER.info("Consultando historia clínica para usuario con cédula: " + cedula);
            
            var documentos = historiaClinicaService.obtenerDocumentosPorCedula(cedula);
            return Response.ok(ApiResponse.success(documentos)).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener documentos de historia clínica", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al obtener la historia clínica"))
                .build();
        }
    }

    /**
     * DEPRECADO: Endpoint anterior que permitía consultar cualquier cédula.
     * Ahora valida que el usuario solo pueda consultar su propia historia.
     * 
     * @deprecated Usar {@link #obtenerMisDocumentos(HttpServletRequest)} en su lugar
     */
    @Deprecated
    @GET
    @Path("/{cedula}/documentos")
    public Response obtenerDocumentosPorCedula(
            @PathParam("cedula") String cedulaParam,
            @Context HttpServletRequest request) {
        try {
            // Extraer cédula del JWT para validar autorización
            Claims claims = (Claims) request.getAttribute("jwtClaims");
            
            if (claims == null) {
                LOGGER.warning("Intento de acceso sin JWT válido a historia clínica");
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Autenticación requerida"))
                    .build();
            }
            
            String cedulaJWT = claims.get("docNumber", String.class);
            
            // VALIDACIÓN CRÍTICA: El usuario solo puede consultar su propia historia clínica
            if (cedulaJWT == null || !cedulaJWT.equals(cedulaParam.trim())) {
                LOGGER.warning("Intento de acceso no autorizado: usuario con cédula " + cedulaJWT + 
                             " intentó acceder a historia clínica de cédula " + cedulaParam);
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.error("No tiene autorización para consultar esta historia clínica"))
                    .build();
            }
            
            LOGGER.info("Consultando historia clínica para usuario con cédula: " + cedulaJWT);
            
            var documentos = historiaClinicaService.obtenerDocumentosPorCedula(cedulaJWT);
            return Response.ok(ApiResponse.success(documentos)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener documentos de historia clínica", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Error interno al obtener la historia clínica"))
                .build();
        }
    }

    public static class RegistrarDocumentoRequest {
        private String tenantId;
        private String cedula;
        private String documentoId;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getCedula() {
            return cedula;
        }

        public void setCedula(String cedula) {
            this.cedula = cedula;
        }

        public String getDocumentoId() {
            return documentoId;
        }

        public void setDocumentoId(String documentoId) {
            this.documentoId = documentoId;
        }
    }

    public static class RegistrarDocumentoResponse {
        private String historiaId;
        private String documentoId;

        public RegistrarDocumentoResponse(String historiaId, String documentoId) {
            this.historiaId = historiaId;
            this.documentoId = documentoId;
        }

        public String getHistoriaId() {
            return historiaId;
        }

        public void setHistoriaId(String historiaId) {
            this.historiaId = historiaId;
        }

        public String getDocumentoId() {
            return documentoId;
        }

        public void setDocumentoId(String documentoId) {
            this.documentoId = documentoId;
        }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}

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
