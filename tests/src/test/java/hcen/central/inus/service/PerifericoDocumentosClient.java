package hcen.central.inus.service;

import hcen.central.inus.dto.DocumentoClinicoDTO;
import jakarta.ejb.Stateless;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stub para IT: devuelve datos en memoria y evita HTTP real.
 */
@Stateless
public class PerifericoDocumentosClient {

    private static final AtomicReference<List<DocumentoClinicoDTO>> batch = new AtomicReference<>(List.of());
    private static final AtomicReference<String> lastPath = new AtomicReference<>("");

    public static void setBatchResponse(List<DocumentoClinicoDTO> docs) {
        batch.set(docs);
    }

    public static String getLastPath() {
        return lastPath.get();
    }

    public static void reset() {
        batch.set(List.of());
        lastPath.set("");
    }

    public Optional<DocumentoClinicoDTO> obtenerDocumento(UUID documentoId, UUID tenantId) {
        // no usado en el IT
        return Optional.empty();
    }

    public List<DocumentoClinicoDTO> obtenerDocumentosBatch(List<UUID> documentoIds, UUID tenantId) {
        lastPath.set("/documentos?ids=" + String.join(",", documentoIds.stream().map(UUID::toString).toList()));
        return new ArrayList<>(batch.get());
    }
}
