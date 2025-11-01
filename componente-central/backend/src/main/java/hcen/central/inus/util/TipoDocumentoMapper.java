package hcen.central.inus.util;

import hcen.central.inus.enums.TipoDocumento;
import java.text.Normalizer;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilidad para normalizar códigos de tipo de documento provenientes de gub.uy
 * y de la base de datos hacia los valores internos DO/PA/OTRO.
 */
public final class TipoDocumentoMapper {

    private static final Logger LOGGER = Logger.getLogger(TipoDocumentoMapper.class.getName());

    private TipoDocumentoMapper() {
        // Utility class
    }

    /**
     * Normaliza cadenas provenientes de gub.uy o la base de datos y las convierte
     * al enum interno {@link TipoDocumento}. Soporta valores crudos como CI, Pasaporte,
     * DNI, OTRO, así como sus variantes con acentos o puntuación.
     *
     * @param rawValue cadena de tipo de documento (puede ser nula)
     * @return enum interno normalizado (DO/PA/OTRO)
     */
    public static TipoDocumento toEnum(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            LOGGER.warning("tipo_documento vacío, usando DO por defecto");
            return TipoDocumento.DO;
        }

        LOGGER.fine(() -> "Normalizando tipo_documento recibido: '" + rawValue + "'");

        String tipoNormalizado = Normalizer.normalize(rawValue.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        String tipoSanitizado = tipoNormalizado.replaceAll("[^A-Z0-9]", "");

        LOGGER.fine(() -> "tipo_documento sanitizado: '" + tipoSanitizado + "'");

        if (tipoSanitizado.isEmpty()) {
            LOGGER.warning("tipo_documento inválido ('" + rawValue + "'), usando OTRO por defecto");
            return TipoDocumento.OTRO;
        }

        try {
            // gub.uy suele enviar variantes como "CI - Cédula de identidad"; limpiamos el prefijo CI.
            if (tipoSanitizado.startsWith("CI")
                    && (tipoSanitizado.contains("CEDULA") || tipoSanitizado.contains("IDENTIDAD"))) {
                return TipoDocumento.DO;
            }

            switch (tipoSanitizado) {
                // Cedula -> Documento (DO)
                case "DO":
                case "CI":
                case "C":
                case "CEDULA":
                case "CEDULAIDENTIDAD":
                case "CEDULADEIDENTIDAD":
                case "CEDULADEIDENTIDADURUGUAYA":
                    return TipoDocumento.DO;

                // Pasaporte -> PA
                case "PA":
                case "PASAPORTE":
                case "PASSPORT":
                case "P":
                    return TipoDocumento.PA;

                // DNI u Otros -> OTRO
                case "OTRO":
                case "OTROS":
                case "DNI":
                case "DNIOTRO":
                case "DOCUMENTONACIONALDEIDENTIDAD":
                case "DOCUMENTONACIONALDEIDENTIDADARGENTINA":
                case "DOCUMENTONACIONALDEIDENTIDADURUGUAY":
                    return TipoDocumento.OTRO;

                default:
                    LOGGER.warning("Tipo de documento desconocido '" + rawValue + "', usando OTRO por defecto");
                    return TipoDocumento.OTRO;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error normalizando tipo de documento: " + rawValue, e);
            return TipoDocumento.OTRO;
        }
    }

    /**
     * Convierte el enum interno a su representación en base de datos.
     * Siempre devuelve DO/PA/OTRO.
     *
     * @param tipo enum interno
     * @return representación String a persistir
     */
    public static String toDatabaseValue(TipoDocumento tipo) {
        return tipo != null ? tipo.name() : TipoDocumento.DO.name();
    }
}
