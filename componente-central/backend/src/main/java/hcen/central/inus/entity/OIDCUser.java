package hcen.central.inus.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity for OpenID Connect authenticated users
 * Maps gub.uy users to local database
 */
@Entity
public class OIDCUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String sub;
    
    private String email;
    private Boolean emailVerified;
    private String fullName;
    private String firstName;
    private String lastName;
    
    private String documentType;
    private String documentNumber;
    
    private String uid;
    private String rid;
    private String nid;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    private Instant updatedAt;
    private Instant lastLogin;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public OIDCUser() {}
    
    public OIDCUser(String sub, String email, String fullName) {
        this.sub = sub;
        this.email = email;
        this.fullName = fullName;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public Boolean isEmailVerified() { return emailVerified; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public String getRid() { return rid; }
    public void setRid(String rid) { this.rid = rid; }
    
    public String getNid() { return nid; }
    public void setNid(String nid) { this.nid = nid; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean isActive() { return active; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Instant getLastLogin() { return lastLogin; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OIDCUser oidcUser = (OIDCUser) o;
        return Objects.equals(sub, oidcUser.sub);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sub);
    }
    
    @Override
    public String toString() {
        return "OIDCUser{" +
                "id=" + id +
                ", sub='" + sub + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", active=" + active +
                '}';
    }
}
