package com.hcen.periferico.service;

import jakarta.ejb.Singleton;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringWriter;

@Singleton
public class XsltService {

    public String transform(InputStream xml, InputStream xsl) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        try { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignore) {}
        try { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); } catch (Exception ignore) {}

        Source xslSrc = new StreamSource(xsl);
        Transformer transformer = factory.newTransformer(xslSrc);

        Source xmlSrc = new StreamSource(xml);
        StringWriter writer = new StringWriter();
        transformer.transform(xmlSrc, new StreamResult(writer));
        return writer.toString();
    }
}

