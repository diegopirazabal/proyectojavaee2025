package hcen.central.inus.enums;

/**
 * Estado de una política de acceso a documento clínico.
 */
public enum EstadoPermiso {
    /**
     * Permiso activo y vigente
     */
    ACTIVO,

    /**
     * Permiso revocado por el paciente antes de su fecha de expiración
     */
    REVOCADO,

    /**
     * Permiso que alcanzó su fecha de expiración
     */
    EXPIRADO
}
