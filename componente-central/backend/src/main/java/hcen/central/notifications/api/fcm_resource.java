package hcen.central.notifications.api;

import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.security.jwt.JWTTokenProvider;
import hcen.central.notifications.dto.ApiResponse;
import hcen.central.notifications.dto.FCMTokenRequest;
import hcen.central.notifications.dto.FCMUnregisterRequest;
import hcen.central.notifications.entity.FCMToken;
import hcen.central.notifications.service.fcm_token_service;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.logging.Logger;

/**
 * Recurso REST para gestión de tokens FCM
 * Implementa los endpoints definidos en openapi-mobile.yaml
 *
 * Endpoints:
 * - POST /api/fcm/register - Registrar token FCM
 * - POST /api/fcm/unregister - Eliminar token FCM
 */
@Stateless
@Path("/fcm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class fcm_resource {

    private static final Logger logger = Logger.getLogger(fcm_resource.class.getName());

    @EJB
    private fcm_token_service fcmTokenService;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

    @EJB
    private JWTTokenProvider jwtTokenProvider;

    /**
     * POST /api/fcm/register
     * Registrar o actualizar token FCM del dispositivo
     *
     * NOTA: Este endpoint requiere autenticación JWT.
     * El usuarioId debe extraerse del token JWT en producción.
     * Por ahora se usa un ID hardcodeado para desarrollo.
     */
    @POST
    @Path("/register")
    public Response registerToken(
            FCMTokenRequest request,
            @Context SecurityContext securityContext,
            @Context HttpHeaders headers) {
        try {
            // Validar request
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.error("El cuerpo de la solicitud es requerido"))
                        .build();
            }

            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.error("El token FCM es requerido"))
                        .build();
            }

            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.error("El deviceId es requerido"))
                        .build();
            }

            Long usuarioId = extractUsuarioId(securityContext, headers);

            logger.info("Registrando token FCM para usuario " + usuarioId +
                       " desde dispositivo " + request.getDeviceId());

            // Invalidar tokens previos del usuario para forzar un registro limpio por sesión
            fcmTokenService.deactivateAllTokensForUser(usuarioId);

            // Registrar el token
            FCMToken savedToken = fcmTokenService.registerToken(
                    usuarioId,
                    request.getToken(),
                    request.getDeviceId(),
                    request.getDeviceModel(),
                    request.getOsVersion()
            );

            logger.info("Token FCM registrado exitosamente: " + savedToken.getId());

            // Retornar respuesta exitosa
            return Response.ok(
                    ApiResponse.success(null, "Token FCM registrado exitosamente")
            ).build();

        } catch (SecurityException e) {
            logger.warning("Usuario no autorizado para registrar token FCM: " + e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Usuario no autenticado"))
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warning("Error de validación al registrar token FCM: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();

        } catch (Exception e) {
            logger.severe("Error interno al registrar token FCM: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error interno del servidor"))
                    .build();
        }
    }

    /**
     * POST /api/fcm/unregister
     * Eliminar token FCM cuando el usuario cierra sesión o desinstala la app
     */
    @POST
    @Path("/unregister")
    public Response unregisterToken(
            FCMUnregisterRequest request,
            @Context SecurityContext securityContext,
            @Context HttpHeaders headers) {
        try {
            // Validar request
            if (request == null || request.getToken() == null || request.getToken().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.error("El token FCM es requerido"))
                        .build();
            }

            Long usuarioId = extractUsuarioId(securityContext, headers);
            logger.info("Eliminando token FCM para usuario " + usuarioId);

            // Eliminar el token
            boolean deleted = fcmTokenService.unregisterToken(request.getToken());

            if (deleted) {
                logger.info("Token FCM eliminado exitosamente");
                return Response.ok(
                        ApiResponse.success(null, "Token FCM eliminado exitosamente")
                ).build();
            } else {
                logger.warning("Token FCM no encontrado para eliminar");
                // Según el contrato OpenAPI, aún retornamos 200 OK
                return Response.ok(
                        ApiResponse.success(null, "Token FCM procesado")
                ).build();
            }

        } catch (SecurityException e) {
            logger.warning("Usuario no autorizado para eliminar token FCM: " + e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Usuario no autenticado"))
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warning("Error de validación al eliminar token FCM: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();

        } catch (Exception e) {
            logger.severe("Error interno al eliminar token FCM: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error interno del servidor"))
                    .build();
        }
    }

    private Long extractUsuarioId(SecurityContext securityContext, HttpHeaders headers) {
        String cedula = null;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            cedula = securityContext.getUserPrincipal().getName();
        } else if (headers != null) {
            String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                cedula = jwtTokenProvider.validateAccessToken(token).getSubject();
            }
        }

        if (cedula == null || cedula.isBlank()) {
            throw new SecurityException("Token JWT requerido");
        }

        final String cedulaFinal = cedula;
        return usuarioSaludDAO.findByCedula(cedulaFinal)
                .map(UsuarioSalud::getId)
                .orElseThrow(() ->
                    new SecurityException("Usuario no encontrado para cédula " + cedulaFinal));
    }
}
