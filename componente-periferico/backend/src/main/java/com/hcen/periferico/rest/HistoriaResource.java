package com.hcen.periferico.rest;

import com.hcen.periferico.dto.documento_historia_dto;
import com.hcen.periferico.service.HistoriaService;
import com.hcen.periferico.service.XsltService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Path("/historia")
public class HistoriaResource {

    @EJB
    private HistoriaService historiaService;

    @EJB
    private XsltService xsltService;

    @POST
    @Path("/{usuario}/documentos")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearDocumento(@PathParam("usuario") String usuario, String body, @HeaderParam("Content-Type") String contentType) {
        try {
            byte[] xmlBytes;
            String title = null;
            if (contentType != null && contentType.toLowerCase().contains("application/xml")) {
                xmlBytes = body.getBytes(StandardCharsets.UTF_8);
            } else {
                // JSON simple: { "xmlBase64": "...", "title": "..." }
                String xmlB64 = extractJsonField(body, "xmlBase64");
                title = extractJsonField(body, "title");
                if (xmlB64 == null) return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse("xmlBase64 requerido")).build();
                xmlBytes = Base64.getDecoder().decode(xmlB64);
            }
            if (title == null || title.isBlank()) title = tryExtractTitle(xmlBytes);
            String id = historiaService.addDocumento(usuario, title, xmlBytes);
            Map<String,Object> resp = new HashMap<>();
            resp.put("id", id);
            resp.put("title", title);
            resp.put("createdAt", java.time.Instant.now().toString());
            return Response.ok(resp).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    @GET
    @Path("/{usuario}/documentos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@PathParam("usuario") String usuario) {
        List<documento_historia_dto> out = new ArrayList<>();
        for (HistoriaService.DocMeta d : historiaService.listByUsuario(usuario)) {
            out.add(new documento_historia_dto(d.id, d.title, d.createdAt, Arrays.asList("xml","html")));
        }
        return Response.ok(out).build();
    }

    @GET
    @Path("/{usuario}/documentos/{id}")
    public Response obtener(@PathParam("usuario") String usuario, @PathParam("id") String id, @QueryParam("format") @DefaultValue("xml") String format) {
        Optional<String> xmlOpt = historiaService.getXml(usuario, id);
        if (xmlOpt.isEmpty()) return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("No encontrado")).build();
        try {
            switch (format.toLowerCase()) {
                case "xml":
                    return Response.ok(new ByteArrayInputStream(xmlOpt.get().getBytes(StandardCharsets.UTF_8))).type(MediaType.APPLICATION_XML).build();
                case "html": {
                    InputStream xmlIs = new ByteArrayInputStream(xmlOpt.get().getBytes(StandardCharsets.UTF_8));
                    InputStream xslIs = getClass().getClassLoader().getResourceAsStream("historia/cda.xsl");
                    if (xslIs == null) return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse("XSL no disponible")).build();
                    String html = xsltService.transform(xmlIs, xslIs);
                    return Response.ok(html).type(MediaType.TEXT_HTML).build();
                }
                case "pdf":
                    return Response.status(Response.Status.NOT_IMPLEMENTED).entity(new ErrorResponse("PDF no disponible")).build();
                default:
                    return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse("Formato inválido")).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse("Error transformando: " + e.getMessage())).build();
        }
    }

    private String extractJsonField(String json, String field) {
        if (json == null) return null;
        // naive simple extractor to avoid extra deps; expects "field":"value"
        String needle = "\"" + field + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;
        int quote1 = json.indexOf('"', colon + 1);
        if (quote1 < 0) return null;
        int quote2 = json.indexOf('"', quote1 + 1);
        if (quote2 < 0) return null;
        return json.substring(quote1 + 1, quote2);
    }

    private String tryExtractTitle(byte[] xml) {
        try {
            String s = new String(xml, StandardCharsets.UTF_8);
            int i = s.indexOf("<title>");
            int j = s.indexOf("</title>", i + 7);
            if (i >= 0 && j > i) return s.substring(i + 7, j).trim();
        } catch (Exception ignore) {}
        return "Documento Clínico";
    }

    public static class ErrorResponse {
        public String error;
        public ErrorResponse() {}
        public ErrorResponse(String e) { this.error = e; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
