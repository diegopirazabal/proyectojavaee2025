package hcen.central.inus.security.pkce;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * Validador de parámetros PKCE
 * Verifica que el code_verifier enviado en el token request coincida
 * con el code_challenge enviado en el authorization request
 */
@Stateless
public class PKCEValidator {
    
    @Inject
    private PKCEGenerator pkceGenerator;
    
    /**
     * Valida que el code_verifier corresponda al code_challenge
     * Regenera el challenge desde el verifier y compara
     * 
     * @param codeVerifier El code_verifier recibido
     * @param originalCodeChallenge El code_challenge almacenado originalmente
     * @return true si coinciden, false si no
     */
    public boolean validateCodeVerifier(String codeVerifier, String originalCodeChallenge) {
        if (codeVerifier == null || codeVerifier.trim().isEmpty()) {
            return false;
        }
        
        if (originalCodeChallenge == null || originalCodeChallenge.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Regenerar el challenge desde el verifier
            String regeneratedChallenge = pkceGenerator.generateCodeChallenge(codeVerifier);
            
            // Comparación segura contra timing attacks
            return constantTimeEquals(regeneratedChallenge, originalCodeChallenge);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Comparación de strings en tiempo constante
     * Previene timing attacks
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
}
