package hcen.central.inus.dto;

/**
 * DTO for authorization request
 * Contains: authorization_url, state, nonce, code_verifier, code_challenge
 */
public class OIDCAuthRequest {
    
    private String authorizationUrl;
    private String state;
    private String nonce;
    private String codeVerifier;
    private String codeChallenge;
    
    public OIDCAuthRequest() {}
    
    public String getAuthorizationUrl() { return authorizationUrl; }
    public void setAuthorizationUrl(String authorizationUrl) { this.authorizationUrl = authorizationUrl; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    
    public String getCodeVerifier() { return codeVerifier; }
    public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }
    
    public String getCodeChallenge() { return codeChallenge; }
    public void setCodeChallenge(String codeChallenge) { this.codeChallenge = codeChallenge; }
}
