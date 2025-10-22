package com.hcen.periferico.service;

import com.hcen.periferico.api.CentralAPIClient;
import com.hcen.periferico.dto.usuario_salud_dto;
import com.hcen.periferico.enums.TipoDocumento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Service para gestión de usuarios de salud.
 * DELEGA todas las operaciones al componente central (INUS).
 * Ya NO persiste datos localmente en la base de datos del tenant.
 */
@Stateless
public class UsuarioSaludService {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludService.class.getName());

    @EJB
    private CentralAPIClient centralClient;

    /**
     * Registra un usuario en una clínica.
     * Delega la operación al componente central.
     */
    public usuario_salud_dto registrarUsuarioEnClinica(String cedula, TipoDocumento tipoDocumento,
                                                       String primerNombre, String segundoNombre,
                                                       String primerApellido, String segundoApellido,
                                                       String email, LocalDate fechaNacimiento,
                                                       String clinicaRut) {
        // Validaciones básicas
        validateRegistroParams(cedula, primerNombre, primerApellido, email, fechaNacimiento, clinicaRut);

        LOGGER.info("Delegando registro de usuario al componente central: " + cedula);

        try {
            // Delegar al componente central
            return centralClient.registrarUsuarioEnClinica(
                cedula,
                tipoDocumento != null ? tipoDocumento : TipoDocumento.CI,
                primerNombre.trim(),
                segundoNombre != null ? segundoNombre.trim() : null,
                primerApellido.trim(),
                segundoApellido != null ? segundoApellido.trim() : null,
                email.trim(),
                fechaNacimiento,
                clinicaRut.trim()
            );
        } catch (Exception e) {
            LOGGER.severe("Error al registrar usuario en componente central: " + e.getMessage());
            throw new RuntimeException("No se pudo registrar el usuario en el sistema central: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si un usuario existe en el componente central
     */
    public boolean verificarUsuarioExiste(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }

        LOGGER.info("Verificando existencia de usuario en componente central: " + cedula);

        try {
            return centralClient.verificarUsuarioExiste(cedula.trim());
        } catch (Exception e) {
            LOGGER.severe("Error al verificar usuario en componente central: " + e.getMessage());
            throw new RuntimeException("No se pudo verificar el usuario en el sistema central: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene un usuario por cédula desde el componente central
     */
    public usuario_salud_dto getUsuarioByCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }

        LOGGER.info("Obteniendo usuario desde componente central: " + cedula);

        try {
            return centralClient.getUsuarioByCedula(cedula.trim());
        } catch (Exception e) {
            LOGGER.severe("Error al obtener usuario desde componente central: " + e.getMessage());
            throw new RuntimeException("No se pudo obtener el usuario del sistema central: " + e.getMessage(), e);
        }
    }

    /**
     * Desasocia un usuario de una clínica eliminando la relación
     * Delega la operación al componente central
     */
    public boolean deleteUsuarioDeClinica(String cedula, String clinicaRut) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (clinicaRut == null || clinicaRut.trim().isEmpty()) {
            throw new IllegalArgumentException("El RUT de la clínica es requerido");
        }

        LOGGER.info("Delegando eliminación de asociación usuario-clínica al componente central: " + cedula);

        try {
            return centralClient.deleteUsuarioDeClinica(cedula.trim(), clinicaRut.trim());
        } catch (Exception e) {
            LOGGER.severe("Error al eliminar asociación en componente central: " + e.getMessage());
            throw new RuntimeException("No se pudo eliminar la asociación en el sistema central: " + e.getMessage(), e);
        }
    }

    /**
     * Validaciones de parámetros de registro
     */
    private void validateRegistroParams(String cedula, String primerNombre, String primerApellido,
                                       String email, LocalDate fechaNacimiento, String clinicaRut) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (primerNombre == null || primerNombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El primer nombre es requerido");
        }
        if (primerApellido == null || primerApellido.trim().isEmpty()) {
            throw new IllegalArgumentException("El primer apellido es requerido");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es requerido");
        }
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es requerida");
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser en el futuro");
        }
        if (clinicaRut == null || clinicaRut.trim().isEmpty()) {
            throw new IllegalArgumentException("El RUT de la clínica es requerido");
        }
    }

    // NOTA: Los siguientes métodos de la versión anterior ya NO se usan porque
    // el sistema ya no persiste usuarios localmente. Se mantienen comentados
    // por si se necesitan en el futuro para caché local o modo offline.

    /*
    public List<usuario_salud> getAllUsuarios() { ... }
    public List<usuario_salud> searchUsuarios(String searchTerm) { ... }
    public void deleteUsuario(Integer ci) { ... }
    public List<usuario_salud> getUsuariosPaginated(int page) { ... }
    etc.
    */
}
