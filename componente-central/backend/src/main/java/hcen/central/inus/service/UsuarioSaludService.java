package hcen.central.inus.service;

import hcen.central.inus.dao.UsuarioClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.entity.UsuarioClinica;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service para gestión de usuarios de salud y su asociación con clínicas
 */
@Stateless
public class UsuarioSaludService {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludService.class.getName());

    @EJB
    private UsuarioSaludDAO usuarioDAO;

    @EJB
    private UsuarioClinicaDAO usuarioClinicaDAO;

    /**
     * Verifica si un usuario existe por cédula
     */
    public boolean verificarUsuarioExiste(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        return usuarioDAO.existsByCedula(cedula.trim());
    }

    /**
     * Registra un usuario en una clínica.
     * Si el usuario NO existe: lo crea en usuario_salud
     * Si el usuario existe: actualiza sus datos
     * Siempre verifica/crea la asociación en usuario_clinica
     */
    public UsuarioSaludDTO registrarUsuarioEnClinica(RegistrarUsuarioRequest request) {
        // Validaciones
        validateRequest(request);

        String cedula = request.getCedula().trim();
        String clinicaRut = request.getClinicaRut().trim();

        // Buscar o crear usuario
        Optional<UsuarioSalud> existingUsuario = usuarioDAO.findByCedula(cedula);
        UsuarioSalud usuario;

        if (existingUsuario.isPresent()) {
            // Usuario existe, actualizar datos
            LOGGER.info("Usuario con cédula " + cedula + " ya existe. Actualizando datos.");
            usuario = existingUsuario.get();
            updateUsuarioData(usuario, request);
            usuario = usuarioDAO.save(usuario);
        } else {
            // Usuario no existe, crear nuevo
            LOGGER.info("Creando nuevo usuario con cédula " + cedula);
            usuario = createNuevoUsuario(request);
            usuario = usuarioDAO.save(usuario);
        }

        // Verificar/crear asociación con clínica
        if (!usuarioClinicaDAO.existsAssociation(cedula, clinicaRut)) {
            LOGGER.info("Creando asociación usuario-clínica: " + cedula + " - " + clinicaRut);
            UsuarioClinica asociacion = new UsuarioClinica(cedula, clinicaRut);
            usuarioClinicaDAO.save(asociacion);
        } else {
            LOGGER.info("La asociación usuario-clínica ya existe: " + cedula + " - " + clinicaRut);
        }

        return toDTO(usuario);
    }

    /**
     * Obtiene datos de un usuario por cédula
     */
    public Optional<UsuarioSaludDTO> getUsuarioByCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        return usuarioDAO.findByCedula(cedula.trim()).map(this::toDTO);
    }

    /**
     * Valida la solicitud de registro
     */
    private void validateRequest(RegistrarUsuarioRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        if (request.getCedula() == null || request.getCedula().trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (request.getPrimerNombre() == null || request.getPrimerNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El primer nombre es requerido");
        }
        if (request.getPrimerApellido() == null || request.getPrimerApellido().trim().isEmpty()) {
            throw new IllegalArgumentException("El primer apellido es requerido");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es requerido");
        }
        if (request.getFechaNacimiento() == null) {
            throw new IllegalArgumentException("La fecha de nacimiento es requerida");
        }
        if (request.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser en el futuro");
        }
        if (request.getClinicaRut() == null || request.getClinicaRut().trim().isEmpty()) {
            throw new IllegalArgumentException("El RUT de la clínica es requerido");
        }
        if (request.getTipoDocumento() == null) {
            request.setTipoDocumento(TipoDocumento.DO); // Default
        }
    }

    /**
     * Crea un nuevo usuario desde la solicitud
     */
    private UsuarioSalud createNuevoUsuario(RegistrarUsuarioRequest request) {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCedula(request.getCedula().trim());
        usuario.setTipoDeDocumento(request.getTipoDocumento());
        usuario.setPrimerNombre(request.getPrimerNombre().trim());
        usuario.setSegundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null);
        usuario.setPrimerApellido(request.getPrimerApellido().trim());
        usuario.setSegundoApellido(request.getSegundoApellido() != null ? request.getSegundoApellido().trim() : null);
        usuario.setEmail(request.getEmail().trim());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setEmailVerificado(false); // No verificado por defecto
        usuario.setActive(true);

        // Construir nombre completo
        String nombreCompleto = buildNombreCompleto(
            request.getPrimerNombre(),
            request.getSegundoNombre(),
            request.getPrimerApellido(),
            request.getSegundoApellido()
        );
        usuario.setNombreCompleto(nombreCompleto);

        return usuario;
    }

    /**
     * Actualiza los datos de un usuario existente
     */
    private void updateUsuarioData(UsuarioSalud usuario, RegistrarUsuarioRequest request) {
        usuario.setPrimerNombre(request.getPrimerNombre().trim());
        usuario.setSegundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null);
        usuario.setPrimerApellido(request.getPrimerApellido().trim());
        usuario.setSegundoApellido(request.getSegundoApellido() != null ? request.getSegundoApellido().trim() : null);
        usuario.setEmail(request.getEmail().trim());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setTipoDeDocumento(request.getTipoDocumento());

        // Actualizar nombre completo
        String nombreCompleto = buildNombreCompleto(
            request.getPrimerNombre(),
            request.getSegundoNombre(),
            request.getPrimerApellido(),
            request.getSegundoApellido()
        );
        usuario.setNombreCompleto(nombreCompleto);
    }

    /**
     * Construye el nombre completo del usuario
     */
    private String buildNombreCompleto(String primerNombre, String segundoNombre,
                                      String primerApellido, String segundoApellido) {
        StringBuilder sb = new StringBuilder();
        sb.append(primerNombre.trim());
        if (segundoNombre != null && !segundoNombre.trim().isEmpty()) {
            sb.append(" ").append(segundoNombre.trim());
        }
        sb.append(" ").append(primerApellido.trim());
        if (segundoApellido != null && !segundoApellido.trim().isEmpty()) {
            sb.append(" ").append(segundoApellido.trim());
        }
        return sb.toString();
    }

    /**
     * Desasocia un usuario de una clínica eliminando la relación
     */
    public boolean desasociarUsuarioDeClinica(String cedula, String clinicaRut) {
        // Validaciones
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (clinicaRut == null || clinicaRut.trim().isEmpty()) {
            throw new IllegalArgumentException("El RUT de la clínica es requerido");
        }

        LOGGER.info("Desasociando usuario " + cedula + " de clínica " + clinicaRut);

        // Verificar que el usuario existe
        if (!verificarUsuarioExiste(cedula)) {
            LOGGER.warning("Usuario no encontrado: " + cedula);
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        // Eliminar la asociación
        boolean deleted = usuarioClinicaDAO.deleteByUsuarioCedulaAndClinicaRut(cedula.trim(), clinicaRut.trim());

        if (deleted) {
            LOGGER.info("Usuario desasociado exitosamente");
        } else {
            LOGGER.warning("No se encontró la asociación usuario-clínica");
        }

        return deleted;
    }

    /**
     * Convierte entidad a DTO
     */
    private UsuarioSaludDTO toDTO(UsuarioSalud entity) {
        UsuarioSaludDTO dto = new UsuarioSaludDTO();
        dto.setId(entity.getId());
        dto.setCedula(entity.getCedula());
        dto.setTipoDocumento(entity.getTipoDeDocumento());
        dto.setFechaNacimiento(entity.getFechaNacimiento());
        dto.setEmail(entity.getEmail());
        dto.setEmailVerificado(entity.getEmailVerificado());
        dto.setNombreCompleto(entity.getNombreCompleto());
        dto.setPrimerNombre(entity.getPrimerNombre());
        dto.setSegundoNombre(entity.getSegundoNombre());
        dto.setPrimerApellido(entity.getPrimerApellido());
        dto.setSegundoApellido(entity.getSegundoApellido());
        dto.setActive(entity.getActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
