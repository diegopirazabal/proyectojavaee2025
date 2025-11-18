package com.hcen.periferico.usuariosalud.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad para decodificar JWT y extraer claims
 * NO valida la firma del JWT, solo decodifica el payload
 */
public class JwtUtil {
    
    private static final Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Extrae un claim del JWT sin validar la firma
     * @param jwt Token JWT
     * @param claimName Nombre del claim
     * @return Valor del claim o null si no existe
     */
    public static String extractClaim(String jwt, String claimName) {
        if (jwt == null || jwt.isBlank()) {
            return null;
        }
        
        try {
            // JWT tiene formato: header.payload.signature
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                LOGGER.warning("JWT no tiene el formato correcto");
                return null;
            }
            
            // Decodificar el payload (segunda parte)
            String payload = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);
            
            // Parsear JSON
            JsonNode jsonNode = objectMapper.readTree(decodedPayload);
            JsonNode claimNode = jsonNode.get(claimName);
            
            if (claimNode != null && !claimNode.isNull()) {
                return claimNode.asText();
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extrayendo claim '" + claimName + "' del JWT", e);
            return null;
        }
    }
    
    /**
     * Extrae el tipo de documento del JWT
     * @param jwt Token JWT
     * @return Tipo de documento (DO, PA, OTRO) o null
     */
    public static String extractDocType(String jwt) {
        return extractClaim(jwt, "docType");
    }
    
    /**
     * Extrae el número de documento del JWT
     * @param jwt Token JWT
     * @return Número de documento o null
     */
    public static String extractDocNumber(String jwt) {
        return extractClaim(jwt, "docNumber");
    }
    
    /**
     * Extrae el subject (userSub) del JWT
     * @param jwt Token JWT
     * @return Subject o null
     */
    public static String extractSubject(String jwt) {
        return extractClaim(jwt, "sub");
    }
    
    /**
     * Extrae el email del JWT
     * @param jwt Token JWT
     * @return Email o null
     */
    public static String extractEmail(String jwt) {
        return extractClaim(jwt, "email");
    }
}
