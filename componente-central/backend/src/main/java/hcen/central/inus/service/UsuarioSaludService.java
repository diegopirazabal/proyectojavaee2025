package hcen.central.inus.service;

import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.dto.ActualizarUsuarioSaludRequest;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service para gestión de usuarios de salud y su asociación con clínicas
 */
@Stateless
public class UsuarioSaludService {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludService.class.getName());
    private static final ZoneId URUGUAY_ZONE = ZoneId.of("America/Montevideo");

    @EJB
    private UsuarioSaludDAO usuarioDAO;

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
     * Registra un usuario de salud en el sistema nacional.
     * Si el usuario ya existe por cédula: devuelve el usuario existente (no es error)
     * Si NO existe: crea un nuevo usuario solo con cédula y tipo de documento
     */
    public UsuarioSaludDTO registrarUsuarioEnClinica(RegistrarUsuarioRequest request) {
        // Validaciones
        validateRequest(request);

        String cedula = request.getCedula().trim();

        // Verificar si ya existe un usuario con esta cédula
        Optional<UsuarioSalud> existente = usuarioDAO.findByCedula(cedula);

        UsuarioSalud usuario;
        if (existente.isPresent()) {
            // Usuario ya registrado - devolver el existente
            LOGGER.info("Usuario con cédula " + cedula + " ya está registrado en el sistema nacional");
            usuario = existente.get();
        } else {
            // Usuario nuevo - crear con datos mínimos
            LOGGER.info("Creando nuevo usuario en sistema nacional con cédula " + cedula);
            usuario = createNuevoUsuarioMinimo(request);
            usuario = usuarioDAO.save(usuario);
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
     * Actualiza los datos de un usuario sin modificar su documento
     */
    public UsuarioSaludDTO actualizarUsuario(String cedula, ActualizarUsuarioSaludRequest request) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de actualización es requerida");
        }

        UsuarioSalud usuario = usuarioDAO.findByCedula(cedula.trim())
            .orElseThrow(() -> new IllegalArgumentException("No existe un usuario con la cédula indicada"));

        if (request.getPrimerNombre() != null) {
            String primerNombre = request.getPrimerNombre().trim();
            if (primerNombre.isEmpty()) {
                throw new IllegalArgumentException("El primer nombre no puede quedar vacío");
            }
            usuario.setPrimerNombre(primerNombre);
        }
        if (request.getSegundoNombre() != null) {
            usuario.setSegundoNombre(request.getSegundoNombre().trim().isEmpty() ? null : request.getSegundoNombre().trim());
        }
        if (request.getPrimerApellido() != null) {
            String primerApellido = request.getPrimerApellido().trim();
            if (primerApellido.isEmpty()) {
                throw new IllegalArgumentException("El primer apellido no puede quedar vacío");
            }
            usuario.setPrimerApellido(primerApellido);
        }
        if (request.getSegundoApellido() != null) {
            usuario.setSegundoApellido(request.getSegundoApellido().trim().isEmpty() ? null : request.getSegundoApellido().trim());
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isEmpty()) {
                throw new IllegalArgumentException("El email no puede quedar vacío");
            }
            usuario.setEmail(email);
        }
        if (request.getActivo() != null) {
            usuario.setActive(request.getActivo());
        }
        if (request.getFechaNacimiento() != null) {
            LocalDate fecha = LocalDate.parse(request.getFechaNacimiento());
            usuario.setFechaNacimiento(fecha);
        }

        usuario.setNombreCompleto(buildNombreCompleto(
            usuario.getPrimerNombre(),
            usuario.getSegundoNombre(),
            usuario.getPrimerApellido(),
            usuario.getSegundoApellido()
        ));

        UsuarioSalud actualizado = usuarioDAO.save(usuario);
        return toDTO(actualizado);
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
        if (request.getTipoDocumento() == null) {
            request.setTipoDocumento(TipoDocumento.DO); // Default
        }
    }

    /**
     * Crea un nuevo usuario con datos mínimos (solo documento)
     */
    private UsuarioSalud createNuevoUsuarioMinimo(RegistrarUsuarioRequest request) {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCedula(request.getCedula().trim());
        usuario.setTipoDeDocumento(request.getTipoDocumento());
        // Datos mínimos requeridos por las columnas NOT NULL
        usuario.setPrimerNombre("PENDIENTE");
        usuario.setPrimerApellido("PENDIENTE");
        usuario.setEmail("pendiente@hcen.gub.uy");
        usuario.setFechaNacimiento(LocalDate.of(1900, 1, 1));
        usuario.setEmailVerificado(false);
        usuario.setActive(true);
        usuario.setNombreCompleto("PENDIENTE PENDIENTE");

        return usuario;
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
