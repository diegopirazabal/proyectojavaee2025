package hcen.central.inus.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoDocumento {
    DO,
    PA,
    OTRO;

    @Deprecated public static final TipoDocumento CI = DO;
    @Deprecated public static final TipoDocumento CEDULA = DO;
    @Deprecated public static final TipoDocumento CEDULA_IDENTIDAD = DO;
    @Deprecated public static final TipoDocumento PASAPORTE = PA;
    @Deprecated public static final TipoDocumento PASSPORT = PA;
    @Deprecated public static final TipoDocumento DNI = OTRO;
    @Deprecated public static final TipoDocumento OTROS = OTRO;

    /**
     * Serializa el enum como string simple (su nombre) en JSON
     */
    @JsonValue
    public String toJson() {
        return this.name();
    }
}