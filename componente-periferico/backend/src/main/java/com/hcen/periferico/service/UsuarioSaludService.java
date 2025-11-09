package com.hcen.periferico.service;

import com.hcen.periferico.dao.SincronizacionPendienteDAO;
import com.hcen.periferico.dao.UsuarioSaludDAO;
import com.hcen.periferico.dto.usuario_salud_dto;
import com.hcen.periferico.entity.SincronizacionPendiente;
import com.hcen.periferico.entity.UsuarioSalud;
import com.hcen.periferico.enums.TipoDocumento;
import com.hcen.periferico.enums.TipoSincronizacion;
import com.hcen.periferico.messaging.UsuarioSaludSincronizacionProducer;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.JMSException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service para gestión de usuarios de salud en el componente periférico.
 *
 * ARQUITECTURA (post-migración a JMS):
 * - PERSISTE localmente en BD periférico (fuente de verdad local)
 * - SINCRONIZA con componente central de forma asíncrona vía JMS (usuario único global)
 * - Usa ActiveMQ Artemis para mensajería bidireccional
 *
 * El periférico consulta SOLO su BD local. La sincronización con el central
 * es para registro nacional, no afecta las operaciones locales.
 */
@Stateless
public class UsuarioSaludService {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSaludService.class.getName());

    @EJB
    private UsuarioSaludDAO usuarioDAO;

    @EJB
    private SincronizacionPendienteDAO sincronizacionDAO;

    /**
     * Productor JMS para sincronización de usuarios con el componente central.
     * Envía mensajes a la cola "UsuarioSaludRegistrado"
     */
    @EJB
    private UsuarioSaludSincronizacionProducer sincronizacionProducer;

    /**
     * Registra un usuario en una clínica (componente periférico).
     *
     * FLUJO:
     * 1. Validar datos
     * 2. Verificar que no exista ya en esta clínica (cedula + tenant_id)
     * 3. Persistir LOCALMENTE en BD periférico
     * 4. Intentar sincronizar con central (asíncrono, no bloquea)
     * 5. Retornar usuario local SIEMPRE (aunque sync falle)
     *
     * @param tenantId UUID del tenant (clínica) - obtenido del admin logueado
     */
    public usuario_salud_dto registrarUsuarioEnClinica(String cedula, TipoDocumento tipoDocumento,
                                                       String primerNombre, String segundoNombre,
                                                       String primerApellido, String segundoApellido,
                                                       String email, LocalDate fechaNacimiento,
                                                       UUID tenantId) {
        // Validaciones
        validateRegistroParams(cedula, primerNombre, primerApellido, email, fechaNacimiento, tenantId);

        String cedulaTrim = cedula.trim();
        LOGGER.info("=== Registrando usuario localmente ===");
        LOGGER.info("Cédula: " + cedulaTrim + ", TenantId: " + tenantId);

        // Verificar duplicado local
        if (usuarioDAO.existsByCedulaAndTenant(cedulaTrim, tenantId)) {
            throw new IllegalArgumentException("El usuario con cédula " + cedulaTrim +
                                               " ya está registrado en esta clínica");
        }

        // Crear entidad local
        UsuarioSalud usuario = new UsuarioSalud(cedulaTrim, tenantId);
        usuario.setTipoDocumento(tipoDocumento != null ? tipoDocumento.name() : TipoDocumento.DO.name());
        usuario.setPrimerNombre(primerNombre.trim());
        usuario.setSegundoNombre(segundoNombre != null ? segundoNombre.trim() : null);
        usuario.setPrimerApellido(primerApellido.trim());
        usuario.setSegundoApellido(segundoApellido != null ? segundoApellido.trim() : null);
        usuario.setEmail(email.trim());
        usuario.setFechaNacimiento(fechaNacimiento);
        usuario.setActive(true);
        usuario.setSincronizadoCentral(false);  // Pendiente de sync

        // Persistir localmente
        usuario = usuarioDAO.save(usuario);
        LOGGER.info("Usuario persistido localmente con éxito");

        // Sincronizar con central vía JMS (asíncrono, no bloquea)
        sincronizarConCentral(usuario);

        // Retornar DTO del usuario local
        return convertirADTO(usuario);
    }

    /**
     * Sincroniza un usuario con el componente central vía JMS.
     * Si falla el envío del mensaje, registra en tabla de auditoría para reintentos.
     *
     * IMPORTANTE: Se ejecuta en una transacción separada (REQUIRES_NEW) para que
     * si falla el envío del mensaje JMS, NO afecte la persistencia local.
     * Esto garantiza que el usuario siempre se guarde localmente aunque el envío falle.
     *
     * NOTA: ActiveMQ Artemis maneja automáticamente los reintentos del mensaje.
     * La tabla sincronizacion_pendiente es solo para auditoría y monitoreo.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sincronizarConCentral(UsuarioSalud usuario) {
        try {
            LOGGER.info("Enviando usuario a cola JMS para sincronización con central: " + usuario.getCedula());

            // Enviar mensaje JMS a la cola
            String messageId = sincronizacionProducer.enviarUsuario(
                    usuario.getCedula(),
                    TipoDocumento.valueOf(usuario.getTipoDocumento())
            );

            LOGGER.info("Usuario enviado exitosamente a cola JMS. MessageID: " + messageId);

            // Registrar en tabla de auditoría (estado PENDIENTE)
            registrarSincronizacionPendiente(usuario, messageId);

        } catch (JMSException e) {
            // Error al enviar mensaje JMS - loguear y registrar en auditoría
            LOGGER.log(Level.SEVERE, "Error al enviar usuario a cola JMS: " + usuario.getCedula(), e);
            registrarSincronizacionError(usuario, "Error JMS: " + e.getMessage());
            // NO propagamos la excepción - la transacción local debe completarse
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al sincronizar usuario con central", e);
            registrarSincronizacionError(usuario, "Error: " + e.getMessage());
            // NO propagamos la excepción - la transacción local debe completarse
        }
    }

    /**
     * Registra una sincronización exitosamente enviada a JMS (estado PENDIENTE).
     * El MDB consumidor actualizará el estado cuando reciba confirmación.
     *
     * @param usuario Usuario que se está sincronizando
     * @param messageId ID del mensaje JMS enviado
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void registrarSincronizacionPendiente(UsuarioSalud usuario, String messageId) {
        try {
            SincronizacionPendiente sync = new SincronizacionPendiente(
                    usuario.getCedula(),
                    usuario.getTenantId(),
                    TipoSincronizacion.USUARIO
            );
            sync.setMessageId(messageId);
            sync.setEstado(SincronizacionPendiente.EstadoSincronizacion.PENDIENTE);

            sincronizacionDAO.save(sync);
            LOGGER.info("Registro de auditoría creado para usuario " + usuario.getCedula() + " (messageId: " + messageId + ")");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar sincronización pendiente", e);
            // No lanzar excepción - el usuario ya está guardado localmente y mensaje JMS ya fue enviado
        }
    }

    /**
     * Registra un error en el envío del mensaje JMS (antes de llegar a ActiveMQ).
     * Esto es diferente a los errores de procesamiento en el central (que son manejados por el MDB).
     *
     * @param usuario Usuario que falló al sincronizar
     * @param errorMensaje Descripción del error
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void registrarSincronizacionError(UsuarioSalud usuario, String errorMensaje) {
        try {
            SincronizacionPendiente sync = new SincronizacionPendiente(
                    usuario.getCedula(),
                    usuario.getTenantId(),
                    TipoSincronizacion.USUARIO
            );
            sync.setEstado(SincronizacionPendiente.EstadoSincronizacion.ERROR);
            sync.setUltimoError(errorMensaje);

            sincronizacionDAO.save(sync);
            LOGGER.info("Error de sincronización registrado para usuario " + usuario.getCedula());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar error de sincronización", e);
            // No lanzar excepción - el usuario ya está guardado localmente
        }
    }

    /**
     * Obtiene todos los usuarios de una clínica (desde BD local)
     */
    public List<usuario_salud_dto> getAllUsuariosByTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }

        List<UsuarioSalud> usuarios = usuarioDAO.findByTenantId(tenantId);
        return usuarios.stream()
                       .map(this::convertirADTO)
                       .collect(Collectors.toList());
    }

    /**
     * Busca usuarios por nombre/apellido en una clínica (desde BD local)
     */
    public List<usuario_salud_dto> searchUsuariosByTenantId(String searchTerm, UUID tenantId) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("El término de búsqueda es requerido");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }

        List<UsuarioSalud> usuarios =
            usuarioDAO.searchByNombreOrApellidoAndTenant(searchTerm, tenantId);
        return usuarios.stream()
                       .map(this::convertirADTO)
                       .collect(Collectors.toList());
    }

    /**
     * Obtiene un usuario por cédula y tenant (desde BD local)
     */
    public usuario_salud_dto getUsuarioByCedulaAndTenant(String cedula, UUID tenantId) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }

        return usuarioDAO.findByCedulaAndTenant(cedula.trim(), tenantId)
                         .map(this::convertirADTO)
                         .orElse(null);
    }

    /**
     * Verifica si un usuario existe en esta clínica (BD local)
     */
    public boolean verificarUsuarioExisteEnClinica(String cedula, UUID tenantId) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }

        return usuarioDAO.existsByCedulaAndTenant(cedula.trim(), tenantId);
    }

    /**
     * Desactiva un usuario de una clínica (soft delete en BD local)
     */
    public void desactivarUsuario(String cedula, UUID tenantId) {
        if (cedula == null || cedula.trim().isEmpty()) {
            throw new IllegalArgumentException("La cédula es requerida");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id es requerido");
        }

        usuarioDAO.desactivar(cedula.trim(), tenantId);
        LOGGER.info("Usuario desactivado: " + cedula + " (tenant: " + tenantId + ")");
    }

    /**
     * Obtiene usuarios pendientes de sincronización con el central
     */
    public List<usuario_salud_dto> getUsuariosPendientesSincronizacion(UUID tenantId) {
        List<UsuarioSalud> pendientes =
            usuarioDAO.findPendientesSincronizacionByTenant(tenantId);
        return pendientes.stream()
                         .map(this::convertirADTO)
                         .collect(Collectors.toList());
    }

    /**
     * Reintenta sincronización de usuarios pendientes (para job programado o manual)
     */
    public void reintentarSincronizacionesPendientes(UUID tenantId) {
        List<UsuarioSalud> pendientes =
            usuarioDAO.findPendientesSincronizacionByTenant(tenantId);

        LOGGER.info("Reintentando sincronización de " + pendientes.size() + " usuarios pendientes");

        for (UsuarioSalud usuario : pendientes) {
            sincronizarConCentral(usuario);
        }
    }

    /**
     * Convierte entidad a DTO
     */
    private usuario_salud_dto convertirADTO(UsuarioSalud usuario) {
        usuario_salud_dto dto = new usuario_salud_dto();
        dto.setCedula(usuario.getCedula());
        dto.setTipoDocumento(TipoDocumento.valueOf(usuario.getTipoDocumento()));
        dto.setPrimerNombre(usuario.getPrimerNombre());
        dto.setSegundoNombre(usuario.getSegundoNombre());
        dto.setPrimerApellido(usuario.getPrimerApellido());
        dto.setSegundoApellido(usuario.getSegundoApellido());
        dto.setEmail(usuario.getEmail());
        dto.setFechaNacimiento(usuario.getFechaNacimiento());
        dto.setTenantId(usuario.getTenantId() != null ? usuario.getTenantId().toString() : null);
        // Agregar campos adicionales si el DTO los soporta
        return dto;
    }

    /**
     * Validaciones de parámetros de registro
     */
    private void validateRegistroParams(String cedula, String primerNombre, String primerApellido,
                                       String email, LocalDate fechaNacimiento, UUID tenantId) {
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
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant_id de la clínica es requerido");
        }
    }
}
