package hcen.central.inus.testsupport.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Timestamp;
import java.time.Instant;

@Converter(autoApply = true)
public class TestInstantAttributeConverter implements AttributeConverter<Instant, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(Instant attribute) {
        Timestamp timestamp = attribute != null ? Timestamp.from(attribute) : null;
        System.out.println(">> TestInstantAttributeConverter.convertToDatabaseColumn(" + attribute + ") -> " + timestamp);
        return timestamp;
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp dbData) {
        return dbData != null ? dbData.toInstant() : null;
    }
}
