package com.hcen.frontend.usuario.service;

import jakarta.enterprise.context.ApplicationScoped;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

@ApplicationScoped
public class xslt_service {

    public String transform(String xmlContent, String xslContent) throws Exception {
        if (xmlContent == null || xslContent == null) {
            throw new IllegalArgumentException("XML/XSL requerido");
        }

        String preparedXsl = ensureXslNamespace(xslContent);
        preparedXsl = sanitizeXmlLike(preparedXsl);

        TransformerFactory factory = TransformerFactory.newInstance();
        try { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignore) {}
        try { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); } catch (Exception ignore) {}

        Source xsl = new StreamSource(new StringReader(preparedXsl));
        Transformer transformer = factory.newTransformer(xsl);

        Source xml = new StreamSource(new StringReader(sanitizeXmlLike(xmlContent)));
        StringWriter writer = new StringWriter();
        transformer.transform(xml, new StreamResult(writer));
        return writer.toString();
    }

    private String ensureXslNamespace(String xslContent) {
        String s = xslContent;
        if (!s.contains("xmlns:xsl") && (s.contains("<xsl:stylesheet") || s.contains("<xsl:transform"))) {
            s = s.replaceFirst("<xsl:stylesheet", "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"");
            s = s.replaceFirst("<xsl:transform", "<xsl:transform xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"");
        }
        return s;
    }

    private String sanitizeXmlLike(String s) {
        if (s == null) return null;
        s = s.replace("\uFEFF", "");
        int idx = s.indexOf('<');
        if (idx > 0) {
            s = s.substring(idx);
        }
        return s;
    }
}

