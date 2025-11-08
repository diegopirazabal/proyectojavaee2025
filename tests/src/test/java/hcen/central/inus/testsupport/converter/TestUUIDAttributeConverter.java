package hcen.central.inus.testsupport.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

@Converter(autoApply = true)
public class TestUUIDAttributeConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(UUID attribute) {
        String value = attribute != null ? attribute.toString() : null;
        System.out.println(">> TestUUIDAttributeConverter.convertToDatabaseColumn(" + attribute + ") -> " + value);
        return value;
    }

    @Override
    public UUID convertToEntityAttribute(String dbData) {
        UUID value = dbData != null ? UUID.fromString(dbData) : null;
        System.out.println(">> TestUUIDAttributeConverter.convertToEntityAttribute(" + dbData + ") -> " + value);
        return value;
    }
}
