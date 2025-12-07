package hcen.central.inus.rest;

import hcen.central.inus.dao.PoliticaAccesoDAO;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST endpoint para estadísticas simples del sistema
 */
@Path("/reportes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportesResource {

    private static final Logger LOGGER =
        Logger.getLogger(ReportesResource.class.getName());

    @EJB
    private PoliticaAccesoDAO politicaAccesoDAO;

    /**
     * Obtiene el total de accesos aprobados (permisos activos)
     *
     * GET /api/reportes/accesos-aprobados
     *
     * @return JSON con { "count": <número> }
     */
    @GET
    @Path("/accesos-aprobados")
    public Response obtenerAccesosAprobados() {
        try {
            long totalAccesos = politicaAccesoDAO.countTotalActivas();

            LOGGER.log(Level.INFO,
                "Total de accesos aprobados: {0}", totalAccesos);

            String jsonResponse = String.format(
                "{\"count\": %d}",
                totalAccesos
            );

            return Response.ok(jsonResponse).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al contar accesos aprobados", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Error al contar accesos\"}")
                .build();
        }
    }

    /**
     * Obtiene accesos aprobados agrupados por clínica
     *
     * GET /api/reportes/accesos-aprobados-por-clinica
     *
     * @return JSON con { "tenantId": count, ... }
     */
    @GET
    @Path("/accesos-aprobados-por-clinica")
    public Response obtenerAccesosAprobadosPorClinica() {
        try {
            Map<java.util.UUID, Long> accesosPorClinica =
                politicaAccesoDAO.countActivasPorClinica();

            LOGGER.log(Level.INFO,
                "Accesos aprobados por {0} clínicas", accesosPorClinica.size());

            // Construir JSON manualmente: { "uuid1": 5, "uuid2": 3, ... }
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<java.util.UUID, Long> entry : accesosPorClinica.entrySet()) {
                if (!first) jsonBuilder.append(",");
                jsonBuilder.append("\"").append(entry.getKey().toString()).append("\":")
                           .append(entry.getValue());
                first = false;
            }
            jsonBuilder.append("}");

            return Response.ok(jsonBuilder.toString()).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener accesos por clínica", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Error al obtener accesos por clínica\"}")
                .build();
        }
    }
}
