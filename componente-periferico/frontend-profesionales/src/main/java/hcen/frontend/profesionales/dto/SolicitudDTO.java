package hcen.frontend.profesionales.dto;

public class SolicitudDTO {
    private String id;
    private String documentoPaciente;
    private String docId;
    private String estado; // PENDIENTE/APROBADA/RECHAZADA

    // getters/setters
    public String getId(){return id;}
    public void setId(String id){this.id=id;}
    public String getDocumentoPaciente(){return documentoPaciente;}
    public void setDocumentoPaciente(String d){this.documentoPaciente=d;}
    public String getDocId(){return docId;}
    public void setDocId(String docId){this.docId=docId;}
    public String getEstado(){return estado;}
    public void setEstado(String e){this.estado=e;}
}
