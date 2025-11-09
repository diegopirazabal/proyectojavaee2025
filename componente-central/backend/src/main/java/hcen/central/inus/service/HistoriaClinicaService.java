package hcen.central.inus.service;

import hcen.central.inus.dao.HistoriaClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.historia_clinica_documento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDateTime;
import java.util.UUID;

@Stateless
public class HistoriaClinicaService {

    @EJB
    private HistoriaClinicaDAO historiaDAO;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

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

        if (!historiaDAO.existsDocumento(historia.getId(), documentoId)) {
            historia_clinica_documento doc = new historia_clinica_documento();
            doc.setHistoriaClinica(historia);
            doc.setDocumentoId(documentoId);
            doc.setUsuarioCedula(cedula.trim());
            doc.setTenantId(tenantId);
            doc.setFecRegistro(LocalDateTime.now());
            historiaDAO.saveDocumento(doc);
            historia.setFecActualizacion(LocalDateTime.now());
            historiaDAO.save(historia);
        }

        return historia.getId();
    }

    private historia_clinica crearHistoria(UsuarioSalud usuario) {
        historia_clinica historia = new historia_clinica();
        historia.setUsuario(usuario);
        historia.setFecCreacion(LocalDateTime.now());
        historia.setFecActualizacion(LocalDateTime.now());
        return historiaDAO.save(historia);
    }
}
