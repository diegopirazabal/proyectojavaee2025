package hcen.central.inus.security.pkce;

import jakarta.ejb.Stateless;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generador de parámetros PKCE (Proof Key for Code Exchange)
 * RFC 7636 - Para proteger el flujo de autorización contra ataques de intercepción
 * 
 * Genera:
 * - code_verifier: String aleatorio de 43-128 caracteres
 * - code_challenge: SHA256(code_verifier) en base64url
 */
@Stateless
public class PKCEGenerator {
    
    private static final String CODE_VERIFIER_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    private static final int CODE_VERIFIER_LENGTH = 128; // RFC 7636: 43-128 caracteres
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Genera un code_verifier aleatorio según RFC 7636
     * @return String de 128 caracteres usando [A-Z, a-z, 0-9, -, ., _, ~]
     */
    public String generateCodeVerifier() {
        StringBuilder verifier = new StringBuilder(CODE_VERIFIER_LENGTH);
        
        for (int i = 0; i < CODE_VERIFIER_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(CODE_VERIFIER_CHARSET.length());
            verifier.append(CODE_VERIFIER_CHARSET.charAt(randomIndex));
        }
        
        return verifier.toString();
    }
    
    /**
     * Genera el code_challenge a partir del code_verifier
     * Método: S256 (SHA-256)
     * @param codeVerifier El code_verifier generado previamente
     * @return code_challenge en Base64URL sin padding
     */
    public String generateCodeChallenge(String codeVerifier) {
        try {
            // SHA-256 del code_verifier
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            
            // Base64URL encode sin padding (RFC 7636)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no está disponible", e);
        }
    }
    
    /**
     * Clase contenedora para code_verifier y code_challenge
     */
    public static class PKCEPair {
        private final String codeVerifier;
        private final String codeChallenge;
        
        public PKCEPair(String codeVerifier, String codeChallenge) {
            this.codeVerifier = codeVerifier;
            this.codeChallenge = codeChallenge;
        }
        
        public String getCodeVerifier() { return codeVerifier; }
        public String getCodeChallenge() { return codeChallenge; }
    }
    
    /**
     * Genera un par completo de PKCE (verifier + challenge)
     * @return PKCEPair con ambos valores
     */
    public PKCEPair generatePKCEPair() {
        String verifier = generateCodeVerifier();
        String challenge = generateCodeChallenge(verifier);
        return new PKCEPair(verifier, challenge);
    }
}
