package com.hcen.periferico.service;

import com.hcen.core.domain.documento_clinico;
import com.hcen.core.domain.historia_clinica;
import com.hcen.core.domain.usuario_salud;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

@Stateless
public class HistoriaService {

    @PersistenceContext(unitName = "hcen-periferico-pu")
    private EntityManager em;

    public static class DocMeta {
        public final String id;
        public final String title;
        public final Instant createdAt;
        public DocMeta(String id, String title, Instant createdAt){ this.id=id; this.title=title; this.createdAt=createdAt; }
    }

    @Transactional
    public String addDocumento(String usuarioCi, String title, byte[] xmlBytes) {
        int ci = parseCi(usuarioCi);
        usuario_salud usuario = em.find(usuario_salud.class, ci);
        if (usuario == null) {
            usuario = new usuario_salud();
            usuario.setCi(ci);
            usuario.setNombre("Desconocido");
            usuario.setApellidos("");
            em.persist(usuario);
        }
        historia_clinica historia = usuario.getHistoriaClinica();
        if (historia == null) {
            historia = new historia_clinica();
            historia.setUsuario(usuario);
            em.persist(historia);
        }
        documento_clinico doc = new documento_clinico();
        doc.setHistoriaClinica(historia);
        doc.setTipo("CDA");
        doc.setTitulo(title != null && !title.isBlank() ? title : "Documento Clínico");
        doc.setContenido(new String(xmlBytes, StandardCharsets.UTF_8));
        em.persist(doc);
        em.flush();
        return doc.getId().toString();
    }

    public List<DocMeta> listByUsuario(String usuarioCi) {
        int ci = parseCi(usuarioCi);
        List<Object[]> rows = em.createQuery(
                "select d.id, d.titulo, d.fecCreacion " +
                "from documento_clinico d " +
                "where d.historiaClinica.usuario.ci = :ci order by d.fecCreacion desc", Object[].class)
            .setParameter("ci", ci)
            .getResultList();
        List<DocMeta> out = new ArrayList<>();
        for (Object[] r : rows) {
            java.util.UUID id = (java.util.UUID) r[0];
            String titulo = (String) r[1];
            java.time.LocalDateTime ldt = (java.time.LocalDateTime) r[2];
            out.add(new DocMeta(id.toString(), titulo, ldt.toInstant(ZoneOffset.UTC)));
        }
        return out;
    }

    public Optional<String> getXml(String usuarioCi, String docId) {
        int ci = parseCi(usuarioCi);
        List<String> res = em.createQuery(
                "select d.contenido from documento_clinico d where d.historiaClinica.usuario.ci = :ci and d.id = :id",
                String.class)
            .setParameter("ci", ci)
            .setParameter("id", java.util.UUID.fromString(docId))
            .setMaxResults(1)
            .getResultList();
        return res.isEmpty() ? Optional.empty() : Optional.ofNullable(res.get(0));
    }

    private int parseCi(String ciStr) {
        try { return Integer.parseInt(ciStr.trim()); } catch (Exception e) { return 0; }
    }
}
