package hcen.central.inus.testsupport.jpa;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.H2Dictionary;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Custom OpenJPA dictionary that makes H2 accept {@link java.util.UUID} parameters.
 * H2 understands UUID literals, but OpenJPA attempts to bind them directly which
 * fails with the in-memory driver. Converting them to strings keeps the backend
 * untouched while letting Arquillian tests run on H2.
 */
public class H2UUIDFriendlyDictionary extends H2Dictionary {

    @Override
    public void setUnknown(PreparedStatement stmnt, int idx, Object val, Column col) throws SQLException {
        super.setUnknown(stmnt, idx, normalizeUUID(val), col);
    }

    @Override
    public void setUnknown(PreparedStatement stmnt, int idx, Column col, Object val) throws SQLException {
        super.setUnknown(stmnt, idx, col, normalizeUUID(val));
    }

    @Override
    public void setObject(PreparedStatement stmnt, int idx, Object val, int jdbcType, Column col) throws SQLException {
        super.setObject(stmnt, idx, normalizeUUID(val), jdbcType, col);
    }

    private Object normalizeUUID(Object val) {
        return val instanceof UUID ? val.toString() : val;
    }
}
