package hcen.central.inus.entity.converter;

import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.util.TipoDocumentoMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter JPA para garantizar que la columna tipo_documento almacene exclusivamente
 * los valores normalizados (DO/PA/OTRO) sin importar los datos crudos recibidos.
 */
@Converter(autoApply = false)
public class TipoDocumentoAttributeConverter implements AttributeConverter<TipoDocumento, String> {

    @Override
    public String convertToDatabaseColumn(TipoDocumento attribute) {
        return TipoDocumentoMapper.toDatabaseValue(attribute);
    }

    @Override
    public TipoDocumento convertToEntityAttribute(String dbData) {
        return TipoDocumentoMapper.toEnum(dbData);
    }
}
