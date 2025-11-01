package hcen.central.inus.dto;

import java.util.List;

/**
 * DTO para respuesta de tokens JWT propios del sistema
 * Se retorna después de autenticación exitosa con gub.uy
 */
public class JWTTokenResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn; // segundos
    private String userSub;
    private List<String> roles;
    private String warningMessage;
    
    public JWTTokenResponse() {}
    
    public JWTTokenResponse(String accessToken, String refreshToken, long expiresIn, String userSub, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.userSub = userSub;
        this.roles = roles;
    }
    
    public JWTTokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, String userSub, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userSub = userSub;
        this.roles = roles;
    }
    
    // Getters/Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    
    public String getUserSub() { return userSub; }
    public void setUserSub(String userSub) { this.userSub = userSub; }
    
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
}
