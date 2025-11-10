package hcen.central.inus.service;

import hcen.central.inus.dao.HistoriaClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.DocumentoClinicoDTO;
import hcen.central.inus.dto.HistoriaClinicaDocumentoDetalleResponse;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.historia_clinica_documento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Stateless
public class HistoriaClinicaService {

    @EJB
    private HistoriaClinicaDAO historiaDAO;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

    @EJB
    private PerifericoDocumentosClient perifericoDocumentosClient;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    public UUID registrarDocumento(String cedula, UUID tenantId, UUID documentoId) {
        if (cedula == null || cedula.isBlank()) {
            throw new IllegalArgumentException("La cédula del paciente es requerida");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenantId es requerido");
        }
        if (documentoId == null) {
            throw new IllegalArgumentException("El documentoId es requerido");
        }

        UsuarioSalud usuario = usuarioSaludDAO.findByCedula(cedula.trim())
            .orElseThrow(() -> new IllegalArgumentException(
                "El usuario con cédula " + cedula + " no existe en el componente central"));

        historia_clinica historia = historiaDAO.findByUsuario(usuario)
            .orElseGet(() -> crearHistoria(usuario));

        if (historia.getUsuarioCedula() == null || historia.getUsuarioCedula().isBlank()) {
            historia.setUsuarioCedula(usuario.getCedula());
            historiaDAO.save(historia);
        }

        if (!historiaDAO.existsDocumento(historia.getId(), documentoId)) {
            historia_clinica_documento doc = new historia_clinica_documento();
            doc.setHistoriaClinica(historia);
            doc.setDocumentoId(documentoId);
            doc.setTenantId(tenantId);
            doc.setFecRegistro(LocalDateTime.now());
            historiaDAO.saveDocumento(doc);
            historia.setFecActualizacion(LocalDateTime.now());
            historiaDAO.save(historia);
        }

        return historia.getId();
    }

    public List<HistoriaClinicaDocumentoDetalleResponse> obtenerDocumentosPorCedula(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        String cedulaNormalizada = cedula.trim();
        UsuarioSalud usuario = usuarioSaludDAO.findByCedula(cedulaNormalizada)
            .orElseThrow(() -> new IllegalArgumentException(
                "No existe el usuario con cédula " + cedulaNormalizada));

        return historiaDAO.findByUsuario(usuario)
            .map(historia -> mapearDocumentos(historia, cedulaNormalizada))
            .orElse(Collections.emptyList());
    }

    private List<HistoriaClinicaDocumentoDetalleResponse> mapearDocumentos(historia_clinica historia, String cedula) {
        List<historia_clinica_documento> documentos = historiaDAO.findDocumentosByHistoria(historia.getId());
        if (documentos == null || documentos.isEmpty()) {
            return Collections.emptyList();
        }
        return documentos.stream()
            .map(doc -> construirDetalle(historia, doc, cedula))
            .collect(Collectors.toList());
    }

    private HistoriaClinicaDocumentoDetalleResponse construirDetalle(
            historia_clinica historia,
            historia_clinica_documento doc,
            String cedula) {

        HistoriaClinicaDocumentoDetalleResponse dto = new HistoriaClinicaDocumentoDetalleResponse();
        dto.setHistoriaId(historia.getId() != null ? historia.getId().toString() : null);
        dto.setDocumentoId(doc.getDocumentoId() != null ? doc.getDocumentoId().toString() : null);
        dto.setTenantId(doc.getTenantId() != null ? doc.getTenantId().toString() : null);
        dto.setUsuarioCedula(cedula);
        dto.setFechaRegistro(doc.getFecRegistro() != null ? doc.getFecRegistro().format(ISO) : null);

        perifericoDocumentosClient.obtenerDocumento(doc.getDocumentoId(), doc.getTenantId())
            .ifPresent(periferico -> aplicarDetalleDesdePeriferico(dto, periferico));

        return dto;
    }

    private void aplicarDetalleDesdePeriferico(
            HistoriaClinicaDocumentoDetalleResponse dto,
            DocumentoClinicoDTO periferico) {

        if (periferico == null) {
            return;
        }

        String motivo = periferico.getNombreMotivoConsulta();
        if (motivo == null || motivo.isBlank()) {
            motivo = periferico.getCodigoMotivoConsulta();
        }
        dto.setMotivoConsulta(motivo);

        String profesional = periferico.getNombreCompletoProfesional();
        if (profesional == null || profesional.isBlank()) {
            if (periferico.getProfesionalCi() != null) {
                profesional = "CI " + periferico.getProfesionalCi();
            }
        }
        dto.setProfesional(profesional);

        String fechaDocumento = periferico.getFechaInicioDiagnostico();
        if (fechaDocumento == null || fechaDocumento.isBlank()) {
            fechaDocumento = periferico.getFecCreacion();
        }
        dto.setFechaDocumento(fechaDocumento);
    }

    private historia_clinica crearHistoria(UsuarioSalud usuario) {
        historia_clinica historia = new historia_clinica();
        historia.setUsuario(usuario);
        historia.setUsuarioCedula(usuario.getCedula());
        historia.setFecCreacion(LocalDateTime.now());
        historia.setFecActualizacion(LocalDateTime.now());
        return historiaDAO.save(historia);
    }
}
