package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.historia_clinica_documento;
import jakarta.ejb.Stateless;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria para los IT, evita las dependencias de JPA/OpenJPA.
 */
@Stateless
public class HistoriaClinicaDAO {

    private final Map<UUID, historia_clinica> historias = new ConcurrentHashMap<>();
    private final Map<UUID, List<historia_clinica_documento>> documentosPorHistoria = new ConcurrentHashMap<>();

    public historia_clinica save(historia_clinica historia) {
        historia.ensureIdForTests();
        historias.put(historia.getId(), historia);
        return historia;
    }

    public Optional<historia_clinica> findById(UUID id) {
        return Optional.ofNullable(historias.get(id));
    }

    public Optional<historia_clinica> findByUsuario(UsuarioSalud usuario) {
        return historias.values().stream()
            .filter(h -> h.getUsuarioCedula().equals(usuario.getCedula()))
            .findFirst();
    }

    public Optional<historia_clinica> findByCedula(String cedula) {
        return historias.values().stream()
            .filter(h -> h.getUsuarioCedula().equals(cedula))
            .findFirst();
    }

    public historia_clinica_documento saveDocumento(historia_clinica_documento doc) {
        doc.ensureIdForTests();
        documentosPorHistoria
            .computeIfAbsent(doc.getHistoriaClinica().getId(), k -> new ArrayList<>())
            .add(doc);
        return doc;
    }

    public boolean existsDocumento(UUID historiaId, UUID documentoId) {
        return documentosPorHistoria.getOrDefault(historiaId, List.of()).stream()
            .anyMatch(d -> documentoId.equals(d.getDocumentoId()));
    }

    public List<historia_clinica_documento> findDocumentosByHistoria(UUID historiaId) {
        return new ArrayList<>(documentosPorHistoria.getOrDefault(historiaId, List.of()));
    }
}
