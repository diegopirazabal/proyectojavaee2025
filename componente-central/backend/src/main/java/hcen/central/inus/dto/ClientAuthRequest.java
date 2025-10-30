package hcen.central.inus.dto;

/**
 * DTO para request de autenticación de clientes (client_credentials flow)
 */
public class ClientAuthRequest {
    
    private String clientId;
    private String clientSecret;
    
    public ClientAuthRequest() {}
    
    public ClientAuthRequest(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    @Override
    public String toString() {
        return "ClientAuthRequest{clientId='" + clientId + "'}";
    }
}
