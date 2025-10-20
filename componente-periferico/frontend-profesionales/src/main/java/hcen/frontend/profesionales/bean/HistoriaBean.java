package hcen.frontend.profesionales.bean;

import hcen.frontend.profesionales.dto.DocumentoDTO;
import hcen.frontend.profesionales.service.APIService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class HistoriaBean {
    private String documentoPaciente;
    private List<DocumentoDTO> documentos = new ArrayList<>();

    @Inject
    private APIService api;

    public void buscarHistoria() {
        documentos = api.listarDocumentosPaciente(documentoPaciente);
    }

    public String verDocumento(DocumentoDTO doc) {
        // TODO: descargar/abrir el documento (local o remoto)
        api.descargarDocumento(doc.getId());
        return null;
    }

    public void solicitarAcceso(DocumentoDTO doc) {
        api.solicitarAcceso(doc.getId());
    }

    // getters/setters
    public String getDocumentoPaciente(){return documentoPaciente;}
    public void setDocumentoPaciente(String d){this.documentoPaciente=d;}
    public List<DocumentoDTO> getDocumentos(){return documentos;}
}
