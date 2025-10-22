package hcen.central.inus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import hcen.central.inus.dto.deserializer.TipoDocumentoDeserializer;

/**
 * DTO para información del usuario obtenida desde el endpoint UserInfo de gub.uy
 * Solo mapea los campos necesarios para UsuarioSalud
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCUserInfo {
    
    // Campos básicos
    private String sub;
    private String email;
    
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    
    // Campos de nombre
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
    
    // Campos de documento (tipo_documento viene como objeto, se deserializa a String)
    @JsonProperty("tipo_documento")
    @JsonDeserialize(using = TipoDocumentoDeserializer.class)
    private String tipoDocumento;
    
    @JsonProperty("numero_documento")
    private String numeroDocumento;
    
    public OIDCUserInfo() {}
    
    // Getters y Setters solo para los campos necesarios
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
    
    // Alias para compatibilidad con JSF dashboard.xhtml
    public String getFullName() { 
        return nombreCompleto != null ? nombreCompleto : 
               (primerNombre != null ? primerNombre + " " : "") + 
               (primerApellido != null ? primerApellido : "");
    }
    
    public String getDocumentType() { return tipoDocumento; }
    public String getDocumentNumber() { return numeroDocumento; }
    
    public String getUid() { return sub; }
}
