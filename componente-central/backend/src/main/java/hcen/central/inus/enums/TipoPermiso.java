package hcen.central.inus.enums;

/**
 * Tipo de permiso de acceso a documento clínico.
 * Define el alcance del acceso otorgado por el paciente.
 */
public enum TipoPermiso {
    /**
     * Acceso otorgado únicamente al profesional específico identificado por su CI
     */
    PROFESIONAL_ESPECIFICO,

    /**
     * Acceso otorgado a todos los profesionales de una especialidad específica
     * dentro de la clínica solicitante
     */
    POR_ESPECIALIDAD,

    /**
     * Acceso otorgado a todos los profesionales de la clínica solicitante,
     * sin importar su especialidad
     */
    POR_CLINICA
}
