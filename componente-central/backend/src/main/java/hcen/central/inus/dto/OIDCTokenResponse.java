package hcen.central.inus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para la respuesta de tokens del token endpoint de gub.uy
 */
public class OIDCTokenResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("id_token")
    private String idToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private Integer expiresIn;
    
    @JsonProperty("scope")
    private String scope;
    
    public OIDCTokenResponse() {}
    
    // Getters/Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public Integer getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }
    
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
