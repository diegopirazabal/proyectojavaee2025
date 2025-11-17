package hcen.central.inus.service;

import hcen.central.inus.dto.DnicCiudadanoDTO;
import hcen.central.inus.exception.CiudadanoNoEncontradoException;
import hcen.central.inus.ws.dnic.*;
import jakarta.ejb.Stateless;
import jakarta.xml.ws.BindingProvider;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente JAX-WS para consumir el servicio SOAP de DNIC.
 *
 * Este servicio encapsula la comunicación con el mock DNIC y mapea
 * las respuestas SOAP a DTOs del dominio de la aplicación.
 */
@Stateless
public class DnicServiceClient {

    private static final Logger LOGGER = Logger.getLogger(DnicServiceClient.class.getName());

    // Endpoint del servicio DNIC (puede configurarse vía system property)
    // Nota: Ya no incluye "?wsdl" porque usamos el WSDL empaquetado
    private static final String DEFAULT_DNIC_ENDPOINT = "http://node5823-hcen-uy.web.elasticloud.uy/mock-dnic/ciudadano-service/ciudadano_servicio_web";
    private static final String DNIC_ENDPOINT_PROPERTY = "dnic.service.endpoint";

    // Formatos de fecha esperados de DNIC (ISO-8601)
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,  // yyyy-MM-dd
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    /**
     * Consulta datos de un ciudadano en DNIC por tipo y número de documento.
     *
     * @param tipoDocumento Tipo de documento (DO, PA, OTRO)
     * @param numeroDocumento Número de documento (8 dígitos)
     * @return DTO con datos del ciudadano
     * @throws CiudadanoNoEncontradoException Si el ciudadano no existe en DNIC
     */
    public DnicCiudadanoDTO obtenerCiudadano(String tipoDocumento, String numeroDocumento)
            throws CiudadanoNoEncontradoException {

        LOGGER.info("Consultando DNIC para: " + tipoDocumento + " " + numeroDocumento);

        try {
            // 1. Crear cliente SOAP
            CiudadanoPortType port = crearClienteSOAP();

            // 2. Construir solicitud
            SolicitudCiudadano solicitud = new SolicitudCiudadano();
            solicitud.setTipoDoc(convertirTipoDocumentoASOAP(tipoDocumento));
            solicitud.setNroDoc(numeroDocumento);

            // 3. Invocar servicio SOAP
            RespuestaCiudadano respuesta = port.obtenerCiudadano(solicitud);

            // 4. Mapear respuesta a DTO
            DnicCiudadanoDTO dto = mapearRespuestaADTO(respuesta);

            LOGGER.info("Ciudadano obtenido exitosamente de DNIC: " + dto.getNombreCompleto());
            return dto;

        } catch (CiudadanoNoEncontradoFaultMessage e) {
            // SOAP Fault: ciudadano no encontrado
            LOGGER.warning("Ciudadano no encontrado en DNIC: " + tipoDocumento + " " + numeroDocumento);
            throw new CiudadanoNoEncontradoException(tipoDocumento, numeroDocumento, e);

        } catch (Exception e) {
            // Error general de comunicación o parsing
            LOGGER.log(Level.SEVERE, "Error consultando DNIC: " + e.getMessage(), e);
            throw new CiudadanoNoEncontradoException(tipoDocumento, numeroDocumento, e);
        }
    }

    /**
     * Crea el cliente SOAP usando el WSDL empaquetado y configurando el endpoint en runtime.
     *
     * Esta implementación separa el contrato (WSDL) del endpoint:
     * - WSDL: Embebido en las clases generadas (build-time)
     * - Endpoint: Configurable vía system property (runtime)
     *
     * Esto permite:
     * 1. No depender de que el servicio esté disponible durante el build
     * 2. Cambiar de endpoint sin recompilar (dev/staging/prod)
     * 3. No requiere modificar standalone.xml del servidor
     *
     * @return Puerto SOAP para invocar operaciones
     */
    private CiudadanoPortType crearClienteSOAP() {
        // 1. Crear servicio usando WSDL empaquetado (constructor sin parámetros)
        CiudadanoService service = new CiudadanoService();
        CiudadanoPortType port = service.getCiudadanoPort();

        // 2. Configurar endpoint del servicio en runtime
        String endpoint = System.getProperty(DNIC_ENDPOINT_PROPERTY, DEFAULT_DNIC_ENDPOINT);

        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(
            BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            endpoint
        );

        LOGGER.info("Cliente SOAP DNIC creado. Endpoint: " + endpoint);

        return port;
    }

    /**
     * Mapea la respuesta SOAP a nuestro DTO de dominio.
     *
     * @param respuesta Respuesta del servicio SOAP
     * @return DTO con datos del ciudadano
     */
    private DnicCiudadanoDTO mapearRespuestaADTO(RespuestaCiudadano respuesta) {
        DnicCiudadanoDTO dto = new DnicCiudadanoDTO();

        // Mapear tipo y número de documento
        dto.setTipoDocumento(convertirTipoDocumentoADominio(respuesta.getTipoDoc()));
        dto.setNumeroDocumento(respuesta.getNroDoc());

        // Mapear nombres y apellidos
        dto.setPrimerNombre(respuesta.getNombre1());
        dto.setSegundoNombre(respuesta.getNombre2());
        dto.setPrimerApellido(respuesta.getApellido1());
        dto.setSegundoApellido(respuesta.getApellido2());

        // Mapear otros datos
        dto.setSexo(respuesta.getSexo());
        dto.setCodigoNacionalidad(respuesta.getCodNacionalidad());
        dto.setNombreEnCedula(respuesta.getNombreEnCedula());

        // Parsear fecha de nacimiento
        dto.setFechaNacimiento(parsearFechaNacimiento(respuesta.getFechaNac()));

        return dto;
    }

    /**
     * Convierte TipoDocumento de String a enum SOAP.
     *
     * @param tipoDocumento Tipo de documento como String (DO, PA, OTRO)
     * @return Enum TipoDocumento del SOAP
     */
    private hcen.central.inus.ws.dnic.TipoDocumento convertirTipoDocumentoASOAP(String tipoDocumento) {
        try {
            return hcen.central.inus.ws.dnic.TipoDocumento.valueOf(tipoDocumento.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Tipo de documento inválido: " + tipoDocumento + ", usando OTRO por defecto");
            return hcen.central.inus.ws.dnic.TipoDocumento.OTRO;
        }
    }

    /**
     * Convierte TipoDocumento del SOAP a nuestro enum de dominio.
     *
     * @param tipoDocSOAP Enum TipoDocumento del SOAP
     * @return Enum TipoDocumento del dominio
     */
    private hcen.central.inus.enums.TipoDocumento convertirTipoDocumentoADominio(
            hcen.central.inus.ws.dnic.TipoDocumento tipoDocSOAP) {

        if (tipoDocSOAP == null) {
            return hcen.central.inus.enums.TipoDocumento.OTRO;
        }

        return switch (tipoDocSOAP) {
            case DO -> hcen.central.inus.enums.TipoDocumento.DO;
            case PA -> hcen.central.inus.enums.TipoDocumento.PA;
            case OTRO -> hcen.central.inus.enums.TipoDocumento.OTRO;
        };
    }

    /**
     * Parsea la fecha de nacimiento desde String a LocalDate.
     * Intenta múltiples formatos para mayor robustez.
     *
     * @param fechaString Fecha como String desde DNIC
     * @return LocalDate parseado, o null si el formato es inválido
     */
    private LocalDate parsearFechaNacimiento(String fechaString) {
        if (fechaString == null || fechaString.isBlank()) {
            LOGGER.warning("Fecha de nacimiento vacía o nula");
            return null;
        }

        // Intentar parsear con múltiples formatos
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate fecha = LocalDate.parse(fechaString, formatter);
                LOGGER.fine("Fecha parseada exitosamente: " + fecha);
                return fecha;
            } catch (DateTimeParseException e) {
                // Intentar siguiente formato
                continue;
            }
        }

        // Si ningún formato funcionó, loguear error y retornar null
        LOGGER.warning("No se pudo parsear fecha de nacimiento: " + fechaString);
        return null;
    }
}
