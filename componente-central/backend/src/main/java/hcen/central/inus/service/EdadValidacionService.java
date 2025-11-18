package hcen.central.inus.service;

import hcen.central.inus.exception.UsuarioMenorDeEdadException;
import jakarta.ejb.Stateless;
import java.time.LocalDate;
import java.time.Period;
import java.util.logging.Logger;

/**
 * Servicio para validación de edad de usuarios.
 *
 * Este servicio centraliza la lógica de validación de mayoría de edad,
 * asegurando que solo usuarios de 18 años o más puedan registrarse.
 */
@Stateless
public class EdadValidacionService {

    private static final Logger LOGGER = Logger.getLogger(EdadValidacionService.class.getName());
    private static final int EDAD_MINIMA = 18;

    /**
     * Calcula la edad en años de una persona dada su fecha de nacimiento.
     *
     * @param fechaNacimiento Fecha de nacimiento del usuario
     * @return Edad en años completos
     */
    public int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            LOGGER.warning("Fecha de nacimiento nula proporcionada");
            return 0;
        }

        LocalDate hoy = LocalDate.now();
        Period periodo = Period.between(fechaNacimiento, hoy);
        int edad = periodo.getYears();

        LOGGER.fine("Edad calculada: " + edad + " años para fecha de nacimiento: " + fechaNacimiento);
        return edad;
    }

    /**
     * Valida que el usuario tenga la edad mínima requerida (18 años).
     *
     * @param fechaNacimiento Fecha de nacimiento del usuario
     * @throws UsuarioMenorDeEdadException Si el usuario es menor de edad
     */
    public void validarMayoriaDeEdad(LocalDate fechaNacimiento) throws UsuarioMenorDeEdadException {
        if (fechaNacimiento == null) {
            LOGGER.warning("Intento de validación con fecha de nacimiento nula");
            throw new UsuarioMenorDeEdadException(null, 0, "Fecha de nacimiento no proporcionada");
        }

        int edad = calcularEdad(fechaNacimiento);

        if (edad < EDAD_MINIMA) {
            LOGGER.warning("Usuario menor de edad detectado: " + edad + " años (requiere " + EDAD_MINIMA + "+)");
            throw new UsuarioMenorDeEdadException(fechaNacimiento, edad);
        }

        LOGGER.info("Validación de edad exitosa: " + edad + " años");
    }

    /**
     * Verifica si el usuario es mayor de edad sin lanzar excepción.
     *
     * @param fechaNacimiento Fecha de nacimiento del usuario
     * @return true si el usuario tiene 18 años o más, false en caso contrario
     */
    public boolean esMayorDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return false;
        }

        int edad = calcularEdad(fechaNacimiento);
        return edad >= EDAD_MINIMA;
    }

    /**
     * Obtiene la edad mínima requerida para el sistema.
     *
     * @return Edad mínima en años
     */
    public int getEdadMinima() {
        return EDAD_MINIMA;
    }
}
