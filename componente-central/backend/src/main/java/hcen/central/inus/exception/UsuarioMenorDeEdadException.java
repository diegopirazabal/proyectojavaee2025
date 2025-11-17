package hcen.central.inus.exception;

import java.time.LocalDate;

/**
 * Excepción lanzada cuando se intenta crear un usuario menor de edad.
 *
 * Según los requisitos del sistema, solo usuarios de 18 años o más
 * pueden registrarse como usuarios de salud.
 */
public class UsuarioMenorDeEdadException extends Exception {

    private static final long serialVersionUID = 1L;

    private LocalDate fechaNacimiento;
    private int edad;

    /**
     * Constructor con fecha de nacimiento y edad calculada.
     *
     * @param fechaNacimiento Fecha de nacimiento del usuario
     * @param edad Edad calculada en años
     */
    public UsuarioMenorDeEdadException(LocalDate fechaNacimiento, int edad) {
        super("Usuario menor de edad: " + edad + " años (se requieren 18 años o más)");
        this.fechaNacimiento = fechaNacimiento;
        this.edad = edad;
    }

    /**
     * Constructor con fecha de nacimiento, edad y mensaje personalizado.
     *
     * @param fechaNacimiento Fecha de nacimiento del usuario
     * @param edad Edad calculada en años
     * @param mensaje Mensaje de error personalizado
     */
    public UsuarioMenorDeEdadException(LocalDate fechaNacimiento, int edad, String mensaje) {
        super(mensaje);
        this.fechaNacimiento = fechaNacimiento;
        this.edad = edad;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public int getEdad() {
        return edad;
    }
}
