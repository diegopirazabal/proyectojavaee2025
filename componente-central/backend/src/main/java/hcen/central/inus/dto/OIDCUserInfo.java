package hcen.central.inus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para información del usuario obtenida desde el endpoint UserInfo de gub.uy
 * Basado en los claims del .well-known
 */
public class OIDCUserInfo {
    
    private String sub;
    private String email;
    
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    
    @JsonProperty("nombre_completo")
    private String nombreCompleto;
    
    @JsonProperty("primer_nombre")
    private String primerNombre;
    
    @JsonProperty("segundo_nombre")
    private String segundoNombre;
    
    @JsonProperty("primer_apellido")
    private String primerApellido;
    
    @JsonProperty("segundo_apellido")
    private String segundoApellido;
    
    @JsonProperty("tipo_documento")
    private String tipoDocumento;
    
    @JsonProperty("numero_documento")
    private String numeroDocumento;
    
    @JsonProperty("pais_documento")
    private String paisDocumento;
    
    private String uid;
    private String rid;
    private String nid;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("given_name")
    private String givenName;
    
    @JsonProperty("family_name")
    private String familyName;
    
    public OIDCUserInfo() {}
    
    // Getters/Setters
    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }
    
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }
    
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }
    
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }
    
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    
    public String getPaisDocumento() { return paisDocumento; }
    public void setPaisDocumento(String paisDocumento) { this.paisDocumento = paisDocumento; }
    
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public String getRid() { return rid; }
    public void setRid(String rid) { this.rid = rid; }
    
    public String getNid() { return nid; }
    public void setNid(String nid) { this.nid = nid; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getGivenName() { return givenName; }
    public void setGivenName(String givenName) { this.givenName = givenName; }
    
    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    
    // English aliases
    public String getFullName() { return nombreCompleto; }
    public void setFullName(String fullName) { this.nombreCompleto = fullName; }
    
    public String getFirstName() { return primerNombre; }
    public void setFirstName(String firstName) { this.primerNombre = firstName; }
    
    public String getLastName() { return primerApellido; }
    public void setLastName(String lastName) { this.primerApellido = lastName; }
    
    public String getDocumentType() { return tipoDocumento; }
    public void setDocumentType(String documentType) { this.tipoDocumento = documentType; }
    
    public String getDocumentNumber() { return numeroDocumento; }
    public void setDocumentNumber(String documentNumber) { this.numeroDocumento = documentNumber; }
}
