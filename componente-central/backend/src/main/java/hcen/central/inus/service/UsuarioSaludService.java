package hcen.central.inus.service;

import hcen.central.inus.dao.UsuarioClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.ActualizarUsuarioSaludRequest;
import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de usuarios de salud y su asociación con clínicas.
 */
@Stateless
public class UsuarioSaludService {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludService.class.getName());
    private static final ZoneId URUGUAY_ZONE = ZoneId.of("America/Montevideo");

    @EJB
    private UsuarioSaludDAO usuarioDAO;

    @EJB
    private UsuarioClinicaDAO usuarioClinicaDAO;

    public boolean verificarUsuarioExiste(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        return usuarioDAO.existsByCedula(cedula.trim());
    }

    /**
     * Registra un usuario en una clínica validando que no exista la combinación cédula + tenant.
     */
    public UsuarioSaludDTO registrarUsuarioEnClinica(RegistrarUsuarioRequest request) {
        validateRequest(request);

        String cedula = request.getCedula().trim();
        UUID tenantId = request.getTenantId();

        if (usuarioDAO.existsByCedulaAndTenantId(cedula, tenantId)) {
            LOGGER.warning(() -> "Usuario con cédula " + cedula + " ya registrado en clínica " + tenantId);
            throw new IllegalArgumentException("El usuario ya está registrado en esta clínica");
        }

        UsuarioSalud usuario = createNuevoUsuario(request);
        usuario.setTenantId(tenantId);
        usuario = usuarioDAO.save(usuario);

        return toDTO(usuario);
    }

    public Optional<UsuarioSaludDTO> getUsuarioByCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        return usuarioDAO.findByCedula(cedula.trim()).map(this::toDTO);
    }

    public java.util.List<UsuarioSaludDTO> getUsuariosByTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
        return usuarioDAO.findByTenantId(tenantId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public java.util.List<UsuarioSaludDTO> searchUsuariosByTenantId(String searchTerm, UUID tenantId) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("El término de búsqueda es requerido");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }
        return usuarioDAO.searchByNombreOrApellidoAndTenantId(searchTerm.trim(), tenantId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

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

    public boolean desasociarUsuarioDeClinica(String cedula, UUID tenantId) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id de la clínica es requerido");
        }

        LOGGER.info(() -> "Desasociando usuario " + cedula + " de clínica " + tenantId);
        boolean deleted = usuarioDAO.deleteByCedulaAndTenantId(cedula.trim(), tenantId);

        if (deleted) {
            LOGGER.info("Usuario desasociado exitosamente");
        } else {
            LOGGER.warning("No se encontró el usuario en esa clínica");
        }
        return deleted;
    }

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
            request.setTipoDocumento(TipoDocumento.DO);
        }
    }

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
        usuario.setEmailVerificado(false);
        usuario.setActive(true);
        usuario.setNombreCompleto(buildNombreCompleto(
            request.getPrimerNombre(),
            request.getSegundoNombre(),
            request.getPrimerApellido(),
            request.getSegundoApellido()
        ));
        return usuario;
    }

    private String buildNombreCompleto(String primerNombre, String segundoNombre,
                                       String primerApellido, String segundoApellido) {
        StringBuilder sb = new StringBuilder();
        sb.append(primerNombre.trim());
        if (segundoNombre != null && !segundoNombre.trim().isEmpty()) {
            sb.append(' ').append(segundoNombre.trim());
        }
        sb.append(' ').append(primerApellido.trim());
        if (segundoApellido != null && !segundoApellido.trim().isEmpty()) {
            sb.append(' ').append(segundoApellido.trim());
        }
        return sb.toString();
    }

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
        dto.setTenantId(entity.getTenantId() != null ? entity.getTenantId().toString() : null);
        return dto;
    }
}
