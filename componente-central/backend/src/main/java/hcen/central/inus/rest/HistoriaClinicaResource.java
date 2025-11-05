package hcen.central.inus.rest;

import hcen.central.inus.service.HistoriaClinicaService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
