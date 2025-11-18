package hcen.central.frontend.usuariosalud.service;

import hcen.central.frontend.usuariosalud.dto.CiudadanoDetalle;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DnicServiceClient implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(DnicServiceClient.class.getName());

    private static final String DNIC_ENDPOINT_ENV = "HCEN_DNIC_ENDPOINT";
    private static final String DNIC_ENDPOINT_PROP = "hcen.dnicEndpoint";
    private static final String DEFAULT_ENDPOINT = "http://179.31.3.190/mock-dnic/ciudadano-service/ciudadano_servicio_web";
    private static final String DNIC_NAMESPACE = "http://hcen.com/soap/ciudadano";

    public CiudadanoDetalle obtenerCiudadano(String tipoDocumento, String numeroDocumento)
            throws DocumentoNoEncontradoException, IOException {

        validarDocumento(tipoDocumento, numeroDocumento);

        HttpPost request = new HttpPost(resolveEndpoint());
        request.addHeader("Content-Type", "text/xml; charset=UTF-8");
        request.addHeader("SOAPAction", "\"obtener_ciudadano\"");
        request.setEntity(new StringEntity(
                construirEnvelope(tipoDocumento, numeroDocumento),
                ContentType.TEXT_XML.withCharset(StandardCharsets.UTF_8)));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {

            String payload;
            try {
                payload = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                        : "";
            } catch (ParseException e) {
                throw new IOException("No se pudo interpretar la respuesta del DNIC", e);
            }

            if (response.getCode() == 200) {
                return parsearCiudadano(payload);
            }

            if (esDocumentoNoEncontrado(payload)) {
                throw new DocumentoNoEncontradoException("Documento no se encuentra en DNIC");
            }

            LOGGER.log(Level.WARNING, "Respuesta inesperada del servicio DNIC. Código {0}, payload: {1}",
                    new Object[]{response.getCode(), payload});
            throw new IOException("Error inesperado al consultar DNIC. Código " + response.getCode());
        }
    }

    private void validarDocumento(String tipoDocumento, String numeroDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
        }
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("El número de documento es obligatorio");
        }
    }

    private String resolveEndpoint() {
        String byEnv = System.getenv(DNIC_ENDPOINT_ENV);
        if (byEnv != null && !byEnv.isBlank()) {
            return byEnv;
        }
        String byProp = System.getProperty(DNIC_ENDPOINT_PROP);
        if (byProp != null && !byProp.isBlank()) {
            return byProp;
        }
        return DEFAULT_ENDPOINT;
    }

    private String construirEnvelope(String tipoDocumento, String numeroDocumento) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ciud="http://hcen.com/soap/ciudadano">
                    <soapenv:Header/>
                    <soapenv:Body>
                        <ciud:solicitud_ciudadano>
                            <ciud:tipoDoc>%s</ciud:tipoDoc>
                            <ciud:nroDoc>%s</ciud:nroDoc>
                        </ciud:solicitud_ciudadano>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(escapeXml(tipoDocumento), escapeXml(numeroDocumento));
    }

    private CiudadanoDetalle parsearCiudadano(String payload) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(payload)));

            Element respuesta = (Element) document.getElementsByTagNameNS(DNIC_NAMESPACE, "respuesta_ciudadano").item(0);
            if (respuesta == null) {
                throw new IOException("La respuesta del DNIC no contiene datos de ciudadano.");
            }

            CiudadanoDetalle detalle = new CiudadanoDetalle();
            detalle.setTipoDocumento(obtenerValor(respuesta, "tipoDoc"));
            detalle.setNumeroDocumento(obtenerValor(respuesta, "nroDoc"));
            detalle.setPrimerNombre(obtenerValor(respuesta, "nombre1"));
            detalle.setSegundoNombre(obtenerValor(respuesta, "nombre2"));
            detalle.setPrimerApellido(obtenerValor(respuesta, "apellido1"));
            detalle.setSegundoApellido(obtenerValor(respuesta, "apellido2"));
            detalle.setSexo(describirSexo(obtenerValor(respuesta, "sexo")));
            detalle.setFechaNacimiento(obtenerValor(respuesta, "fechaNac"));
            detalle.setCodigoNacionalidad(obtenerValor(respuesta, "codNacionalidad"));
            detalle.setNombreEnCedula(obtenerValor(respuesta, "nombreEnCedula"));
            return detalle;
        } catch (ParserConfigurationException e) {
            throw new IOException("No se pudo inicializar el parser XML", e);
        } catch (Exception e) {
            throw new IOException("Error interpretando la respuesta del DNIC", e);
        }
    }

    private String obtenerValor(Element respuesta, String nombreElemento) {
        Element elemento = (Element) respuesta.getElementsByTagNameNS(DNIC_NAMESPACE, nombreElemento).item(0);
        return elemento != null ? elemento.getTextContent() : "";
    }

    private String describirSexo(String sexoCodigo) {
        if (sexoCodigo == null || sexoCodigo.isBlank()) {
            return "";
        }
        return switch (sexoCodigo.trim()) {
            case "1" -> "Masculino";
            case "2" -> "Femenino";
            default -> sexoCodigo;
        };
    }

    private boolean esDocumentoNoEncontrado(String payload) {
        return payload != null && payload.contains("ciudadano_no_encontrado");
    }

    private String escapeXml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
