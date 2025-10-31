package hcen.frontend.admin.bean;

import hcen.frontend.admin.dto.usuario_sistema_dto;
import hcen.frontend.admin.service.api_service;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@ViewScoped
public class usuarios_bean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(usuarios_bean.class.getName());
    private static final long serialVersionUID = 1L;

    private static final List<String> TIPOS_DOCUMENTO = Arrays.asList("DO", "PA", "OTRO");

    @Inject
    private api_service apiService;

    private List<usuario_sistema_dto> usuarios = new ArrayList<>();

    private String filtroTipoDoc = "DO";
    private String filtroNumeroDoc;
    private String filtroNombre;
    private String filtroApellido;

    private usuario_sistema_dto usuarioSeleccionado;
    private usuario_sistema_dto usuarioEdicion;

    @PostConstruct
    public void init() {
        cargarUsuariosIniciales();
    }

    public void cargarUsuariosIniciales() {
        try {
            usuarios = apiService.obtenerUsuariosSistema(null, null, null, null);
        } catch (Exception e) {
            usuarios = new ArrayList<>();
            LOGGER.log(Level.SEVERE, "No se pudo cargar el catálogo inicial", e);
            addError("Error", "No se pudo cargar el catálogo de usuarios");
        }
    }

    public void buscarPorDocumento() {
        if (filtroNumeroDoc == null || filtroNumeroDoc.isBlank()) {
            addError("Dato requerido", "Debe ingresar el número de documento");
            return;
        }
        try {
            usuarios = apiService.obtenerUsuariosSistema(filtroTipoDoc, filtroNumeroDoc.trim(), null, null);
            if (usuarios.isEmpty()) {
                addInfo("Sin resultados", "No se encontraron usuarios para el documento indicado");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error buscando por documento", e);
            addError("Error", "No se pudo recuperar la información del documento indicado");
        }
    }

    public void buscarPorNombre() {
        if ((filtroNombre == null || filtroNombre.isBlank()) && (filtroApellido == null || filtroApellido.isBlank())) {
            addError("Dato requerido", "Debe ingresar nombre y/o apellido para buscar");
            return;
        }
        try {
            usuarios = apiService.obtenerUsuariosSistema(null, null,
                    filtroNombre != null ? filtroNombre.trim() : null,
                    filtroApellido != null ? filtroApellido.trim() : null);
            if (usuarios.isEmpty()) {
                addInfo("Sin resultados", "No se encontraron usuarios con los filtros ingresados");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error buscando por nombre", e);
            addError("Error", "No se pudo recuperar la información con los filtros indicados");
        }
    }

    public void limpiarFiltros() {
        filtroNumeroDoc = null;
        filtroNombre = null;
        filtroApellido = null;
        filtroTipoDoc = "DO";
        cargarUsuariosIniciales();
    }

    public void prepararEdicion(usuario_sistema_dto usuario) {
        if (usuario == null) {
            return;
        }
        this.usuarioSeleccionado = usuario;
        this.usuarioEdicion = copiarUsuario(usuario);
        PrimeFaces.current().ajax().update("usuariosForm:dialogoEdicion");
        PrimeFaces.current().executeScript("PF('dialogoEdicion').show()");
    }

    public void guardarCambios() {
        if (usuarioEdicion == null) {
            addError("Error", "No hay usuario seleccionado para editar");
            return;
        }

        boolean actualizado;
        if (usuarioEdicion.esUsuarioSalud()) {
            actualizado = apiService.actualizarUsuarioSalud(usuarioEdicion);
        } else if (usuarioEdicion.esProfesionalSalud()) {
            actualizado = apiService.actualizarProfesional(usuarioEdicion);
        } else if (usuarioEdicion.esAdministradorClinica()) {
            actualizado = apiService.actualizarAdministradorClinica(usuarioEdicion);
        } else {
            addError("Tipo desconocido", "No se reconoce el tipo de usuario seleccionado");
            return;
        }

        if (actualizado) {
            recomponerNombreCompleto(usuarioEdicion);
            copiarSobreOriginal(usuarioSeleccionado, usuarioEdicion);
            addInfo("Cambios guardados", "Se actualizó la información correctamente");
            PrimeFaces.current().executeScript("PF('dialogoEdicion').hide()");
            PrimeFaces.current().ajax().update("usuariosForm:tablaUsuarios", "usuariosForm:messages");
        } else {
            addError("Error", "No se pudo guardar la información. Revise los datos e intente nuevamente");
        }
    }

    public List<usuario_sistema_dto> getUsuarios() {
        return usuarios;
    }

    public List<String> getTiposDocumento() {
        return TIPOS_DOCUMENTO;
    }

    public String getFiltroTipoDoc() {
        return filtroTipoDoc;
    }

    public void setFiltroTipoDoc(String filtroTipoDoc) {
        this.filtroTipoDoc = filtroTipoDoc;
    }

    public String getFiltroNumeroDoc() {
        return filtroNumeroDoc;
    }

    public void setFiltroNumeroDoc(String filtroNumeroDoc) {
        this.filtroNumeroDoc = filtroNumeroDoc;
    }

    public String getFiltroNombre() {
        return filtroNombre;
    }

    public void setFiltroNombre(String filtroNombre) {
        this.filtroNombre = filtroNombre;
    }

    public String getFiltroApellido() {
        return filtroApellido;
    }

    public void setFiltroApellido(String filtroApellido) {
        this.filtroApellido = filtroApellido;
    }

    public usuario_sistema_dto getUsuarioEdicion() {
        return usuarioEdicion;
    }

    public boolean isUsuarioSaludSeleccionado() {
        return usuarioEdicion != null && usuarioEdicion.esUsuarioSalud();
    }

    public boolean isProfesionalSeleccionado() {
        return usuarioEdicion != null && usuarioEdicion.esProfesionalSalud();
    }

    public boolean isAdministradorSeleccionado() {
        return usuarioEdicion != null && usuarioEdicion.esAdministradorClinica();
    }

    public String obtenerClaseFila(usuario_sistema_dto usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return "usuario-row";
        }
        if (usuario.esUsuarioSalud()) {
            return "usuario-salud-row";
        }
        if (usuario.esProfesionalSalud()) {
            return "usuario-profesional-row";
        }
        if (usuario.esAdministradorClinica()) {
            return "usuario-admin-row";
        }
        return "usuario-row";
    }

    private usuario_sistema_dto copiarUsuario(usuario_sistema_dto original) {
        usuario_sistema_dto copia = new usuario_sistema_dto();
        copia.setId(original.getId());
        copia.setOrigen(original.getOrigen());
        copia.setTipoUsuario(original.getTipoUsuario());
        copia.setTipoDocumento(original.getTipoDocumento());
        copia.setNumeroDocumento(original.getNumeroDocumento());
        copia.setPrimerNombre(original.getPrimerNombre());
        copia.setSegundoNombre(original.getSegundoNombre());
        copia.setPrimerApellido(original.getPrimerApellido());
        copia.setSegundoApellido(original.getSegundoApellido());
        copia.setNombreCompleto(original.getNombreCompleto());
        copia.setEmail(original.getEmail());
        copia.setActivo(original.getActivo());
        copia.setFechaNacimiento(original.getFechaNacimiento());
        copia.setEspecialidad(original.getEspecialidad());
        copia.setTenantId(original.getTenantId());
        copia.setUsername(original.getUsername());
        return copia;
    }

    private void copiarSobreOriginal(usuario_sistema_dto destino, usuario_sistema_dto origen) {
        destino.setPrimerNombre(origen.getPrimerNombre());
        destino.setSegundoNombre(origen.getSegundoNombre());
        destino.setPrimerApellido(origen.getPrimerApellido());
        destino.setSegundoApellido(origen.getSegundoApellido());
        destino.setNombreCompleto(origen.getNombreCompleto());
        destino.setEmail(origen.getEmail());
        destino.setActivo(origen.getActivo());
        destino.setFechaNacimiento(origen.getFechaNacimiento());
        destino.setEspecialidad(origen.getEspecialidad());
        destino.setTenantId(origen.getTenantId());
        destino.setUsername(origen.getUsername());
    }

    public String obtenerEtiquetaTipo(usuario_sistema_dto usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return "Desconocido";
        }
        if (usuario.esUsuarioSalud()) {
            return "Usuario de salud";
        }
        if (usuario.esProfesionalSalud()) {
            return "Profesional de salud";
        }
        if (usuario.esAdministradorClinica()) {
            return "Administrador de clínica";
        }
        return usuario.getTipoUsuario();
    }

    private void recomponerNombreCompleto(usuario_sistema_dto usuario) {
        StringBuilder builder = new StringBuilder();
        if (usuario.getPrimerNombre() != null && !usuario.getPrimerNombre().isBlank()) {
            builder.append(usuario.getPrimerNombre().trim());
        }
        if (usuario.getSegundoNombre() != null && !usuario.getSegundoNombre().isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(usuario.getSegundoNombre().trim());
        }
        if (usuario.getPrimerApellido() != null && !usuario.getPrimerApellido().isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(usuario.getPrimerApellido().trim());
        }
        if (usuario.getSegundoApellido() != null && !usuario.getSegundoApellido().isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(usuario.getSegundoApellido().trim());
        }
        usuario.setNombreCompleto(builder.toString());
    }

    private void addInfo(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addError(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }
}
