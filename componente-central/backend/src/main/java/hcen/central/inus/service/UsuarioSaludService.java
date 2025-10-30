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
     * Verifica si ya existe la combinación cedula+tenant_id.
     * Si YA existe: retorna error indicando que ya está registrado en esa clínica
     * Si NO existe: crea nuevo usuario en usuario_salud con todos los datos
     */
    public UsuarioSaludDTO registrarUsuarioEnClinica(RegistrarUsuarioRequest request) {
        // Validaciones
        validateRequest(request);

        String cedula = request.getCedula().trim();
        java.util.UUID tenantId = request.getTenantId();

        // Verificar si ya existe un usuario con esta cedula+tenant_id
        if (usuarioDAO.existsByCedulaAndTenantId(cedula, tenantId)) {
            LOGGER.warning("Usuario con cédula " + cedula + " ya está registrado en la clínica " + tenantId);
            throw new IllegalArgumentException("El usuario ya está registrado en esta clínica");
        }

        // Crear nuevo usuario en usuario_salud
        LOGGER.info("Creando nuevo usuario con cédula " + cedula + " y tenant_id " + tenantId);
        UsuarioSalud usuario = createNuevoUsuario(request);
        usuario.setTenantId(tenantId);
        usuario = usuarioDAO.save(usuario);

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
     * Lista todos los usuarios de una clínica
     */
    public java.util.List<UsuarioSaludDTO> getUsuariosByTenantId(java.util.UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
        return usuarioDAO.findByTenantId(tenantId).stream()
            .map(this::toDTO)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Busca usuarios por nombre o apellido filtrados por tenant_id
     */
    public java.util.List<UsuarioSaludDTO> searchUsuariosByTenantId(String searchTerm, java.util.UUID tenantId) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("El término de búsqueda es requerido");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
        return usuarioDAO.searchByNombreOrApellidoAndTenantId(searchTerm.trim(), tenantId).stream()
            .map(this::toDTO)
            .collect(java.util.stream.Collectors.toList());
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
        LocalDate today = LocalDate.now(URUGUAY_ZONE);
        if (request.getFechaNacimiento().isAfter(today)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser en el futuro");
        }
        if (Period.between(request.getFechaNacimiento(), today).getYears() < 18) {
            LOGGER.warning(() -> "Registro permitido para usuario menor de edad: " + request.getCedula());
        }
        if (request.getTenantId() == null) {
            throw new IllegalArgumentException("El ID de la clínica (tenant_id) es requerido");
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
     * Desasocia un usuario de una clínica eliminando el registro
     */
    public boolean desasociarUsuarioDeClinica(String cedula, java.util.UUID tenantId) {
        // Validaciones
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id de la clínica es requerido");
        }

        LOGGER.info("Desasociando usuario " + cedula + " de clínica " + tenantId);

        // Buscar y eliminar el usuario con esa combinación cedula+tenant_id
        boolean deleted = usuarioDAO.deleteByCedulaAndTenantId(cedula.trim(), tenantId);

        if (deleted) {
            LOGGER.info("Usuario desasociado exitosamente");
        } else {
            LOGGER.warning("No se encontró el usuario en esa clínica");
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
