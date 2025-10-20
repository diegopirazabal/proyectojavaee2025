package hcen.frontend.profesionales.bean;

import hcen.frontend.profesionales.service.APIService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.file.UploadedFile;

@Named
@RequestScoped
public class DocumentoBean {

    private String documentoPaciente;
    private String tipo;
    private UploadedFile file;   // <- en lugar de FileUploadEvent
    private byte[] contenido;

    @Inject
    private APIService api;

    public void registrar() {
        try {
            if (file != null && file.getContent() != null) {
                contenido = file.getContent(); // byte[]
            }
            api.altaDocumento(documentoPaciente, tipo, contenido);
            // Podés agregar un FacesMessage de éxito si querés
        } catch (Exception e) {
            throw new RuntimeException("Error registrando documento", e);
        }
    }

    // getters/setters
    public String getDocumentoPaciente() { return documentoPaciente; }
    public void setDocumentoPaciente(String documentoPaciente) { this.documentoPaciente = documentoPaciente; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public UploadedFile getFile() { return file; }
    public void setFile(UploadedFile file) { this.file = file; }
}
