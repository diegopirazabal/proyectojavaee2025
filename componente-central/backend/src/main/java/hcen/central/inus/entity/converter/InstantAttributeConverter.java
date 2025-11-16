package hcen.central.inus.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Converts {@link Instant} attributes to {@link Timestamp} for databases that
 * expect SQL temporal types.
 */
@Converter(autoApply = true)
public class InstantAttributeConverter implements AttributeConverter<Instant, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(Instant attribute) {
        return attribute != null ? Timestamp.from(attribute) : null;
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp dbData) {
        return dbData != null ? dbData.toInstant() : null;
    }
}
