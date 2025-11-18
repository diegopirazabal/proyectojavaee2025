package hcen.central.notifications.service;

import hcen.central.notifications.dao.fcm_token_dao;
import hcen.central.notifications.entity.FCMToken;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Servicio de lógica de negocio para gestión de tokens FCM
 */
@Stateless
public class fcm_token_service {

    private static final Logger logger = Logger.getLogger(fcm_token_service.class.getName());

    @EJB
    private fcm_token_dao dao;

    /**
     * Registrar o actualizar un token FCM para un usuario
     * Si el token ya existe, se actualiza
     * Si existe un token para ese usuario y dispositivo, se actualiza
     */
    public FCMToken registerToken(Long usuarioId, String fcmToken, String deviceId,
                                   String deviceModel, String osVersion) {

        if (usuarioId == null || fcmToken == null || fcmToken.trim().isEmpty()) {
            throw new IllegalArgumentException("usuarioId y fcmToken son requeridos");
        }

        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("deviceId es requerido");
        }

        // Verificar si ya existe un token exacto
        Optional<FCMToken> existingToken = dao.findByFcmToken(fcmToken);
        if (existingToken.isPresent()) {
            FCMToken token = existingToken.get();
            // Reasignar token al usuario autenticado
            token.setUsuarioId(usuarioId);
            token.setDeviceModel(deviceModel);
            token.setOsVersion(osVersion);
            token.setActive(true);
            logger.info("Actualizando token FCM existente para usuario " + usuarioId);
            return dao.update(token);
        }

        // Verificar si existe un token para este usuario y dispositivo
        Optional<FCMToken> deviceToken = dao.findByUsuarioAndDevice(usuarioId, deviceId);
        if (deviceToken.isPresent()) {
            FCMToken token = deviceToken.get();
            // Actualizar con el nuevo token
            token.setFcmToken(fcmToken);
            token.setUsuarioId(usuarioId);
            token.setDeviceModel(deviceModel);
            token.setOsVersion(osVersion);
            token.setActive(true);
            logger.info("Actualizando token FCM para dispositivo " + deviceId + " del usuario " + usuarioId);
            return dao.update(token);
        }

        // Crear nuevo registro
        FCMToken newToken = new FCMToken(usuarioId, fcmToken, deviceId);
        newToken.setDeviceModel(deviceModel);
        newToken.setOsVersion(osVersion);
        newToken.setActive(true);

        logger.info("Registrando nuevo token FCM para usuario " + usuarioId);
        return dao.save(newToken);
    }

    /**
     * Eliminar un token FCM
     */
    public boolean unregisterToken(String fcmToken) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            throw new IllegalArgumentException("fcmToken es requerido");
        }

        boolean deleted = dao.deleteByFcmToken(fcmToken);
        if (deleted) {
            logger.info("Token FCM eliminado exitosamente");
        } else {
            logger.warning("Token FCM no encontrado para eliminar: " + fcmToken);
        }
        return deleted;
    }

    /**
     * Desactivar un token (soft delete)
     */
    public boolean deactivateToken(String fcmToken) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            throw new IllegalArgumentException("fcmToken es requerido");
        }

        boolean deactivated = dao.deactivateByFcmToken(fcmToken);
        if (deactivated) {
            logger.info("Token FCM desactivado exitosamente");
        } else {
            logger.warning("Token FCM no encontrado para desactivar: " + fcmToken);
        }
        return deactivated;
    }

    /**
     * Obtener todos los tokens activos de un usuario
     */
    public List<FCMToken> getActiveTokensForUser(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("usuarioId es requerido");
        }
        return dao.findActiveTokensByUsuarioId(usuarioId);
    }

    /**
     * Desactivar todos los tokens de un usuario (útil para logout global)
     */
    public int deactivateAllTokensForUser(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("usuarioId es requerido");
        }

        int count = dao.deactivateAllByUsuarioId(usuarioId);
        logger.info("Desactivados " + count + " tokens para usuario " + usuarioId);
        return count;
    }

    /**
     * Verificar si un token existe y está activo
     */
    public boolean isTokenActive(String fcmToken) {
        Optional<FCMToken> token = dao.findByFcmToken(fcmToken);
        return token.isPresent() && token.get().isActive();
    }

    /**
     * Contar tokens activos de un usuario
     */
    public long countActiveTokensForUser(Long usuarioId) {
        if (usuarioId == null) {
            return 0;
        }
        return dao.countActiveTokensByUsuarioId(usuarioId);
    }

    /**
     * Obtener todos los tokens activos del sistema
     * Usado para notificaciones broadcast a todos los usuarios
     */
    public List<FCMToken> getAllActiveTokens() {
        List<FCMToken> tokens = dao.findAllActiveTokens();
        logger.info("Obtenidos " + tokens.size() + " tokens activos del sistema");
        return tokens;
    }
}
