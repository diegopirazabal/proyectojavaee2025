package com.hcen.periferico.profesional.bean;

import com.hcen.periferico.profesional.dto.documento_clinico_dto;
import com.hcen.periferico.profesional.dto.usuario_salud_dto;
import com.hcen.periferico.profesional.service.APIService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.primefaces.PrimeFaces;

/**
 * Managed Bean para gestión de documentos clínicos ambulatorios.
 * Para uso exclusivo de profesionales de salud.
 */
@Named
@SessionScoped
public class DocumentoClinicoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private APIService apiService;

    @Inject
    private SessionBean sessionBean;

    // Listas
    private List<documento_clinico_dto> documentos;
    private List<usuario_salud_dto> pacientes;

    // Catálogos (codigueras)
    private Map<String, String> motivosConsulta;
    private Map<String, String> estadosProblema;
    private Map<String, String> gradosCerteza;

    // Documento en edición
    private documento_clinico_dto newDocumento;
    private documento_clinico_dto selectedDocumento;

    // Filtros
    private String cedulaPacienteSeleccionado;
    private usuario_salud_dto pacienteSeleccionado;

    // Autocompletado
    private String motivoConsultaAutoComplete; // Para el componente p:autoComplete (solo el nombre)
    private Map<String, String> motivosCache; // Cache de nombre -> código para autocompletado

    @PostConstruct
    public void init() {
        newDocumento = new documento_clinico_dto();
        selectedDocumento = new documento_clinico_dto();
        documentos = new ArrayList<>();
        pacientes = new ArrayList<>();
        motivosCache = new LinkedHashMap<>();

        cargarCatalogos();
        cargarPacientes();
    }

    /**
     * Carga los catálogos de codigueras desde el backend
     */
    public void cargarCatalogos() {
        try {
            motivosConsulta = apiService.getMotivosConsulta();
            estadosProblema = apiService.getEstadosProblema();
            gradosCerteza = apiService.getGradosCerteza();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar catálogos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga la lista de pacientes
     */
    public void cargarPacientes() {
        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                return;
            }
            List<usuario_salud_dto> resultado = apiService.getAllUsuarios(tenantId);
            List<usuario_salud_dto> filtrado = resultado.stream()
                .filter(p -> p.getTenantId() != null && tenantId.equalsIgnoreCase(p.getTenantId()))
                .collect(Collectors.toList());
            if (filtrado.isEmpty() && !resultado.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_WARN, "No se encontraron pacientes asociados al tenant actual.");
            }
            pacientes = filtrado;
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar pacientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga documentos de un paciente específico
     */
    public void cargarDocumentosPorPaciente() {
        if (cedulaPacienteSeleccionado == null || cedulaPacienteSeleccionado.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Debe seleccionar un paciente");
            return;
        }

        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                return;
            }

            documentos = apiService.getDocumentosPorPaciente(cedulaPacienteSeleccionado, UUID.fromString(tenantId));

            // Buscar el paciente seleccionado para mostrar su nombre
            for (usuario_salud_dto paciente : pacientes) {
                if (paciente.getCedula().equals(cedulaPacienteSeleccionado)) {
                    pacienteSeleccionado = paciente;
                    break;
                }
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar documentos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prepara el formulario para crear un nuevo documento
     */
    public void prepararNuevoDocumento() {
        newDocumento = new documento_clinico_dto();
        motivoConsultaAutoComplete = null; // Reset del autocompletado

        // Pre-llenar datos
        if (cedulaPacienteSeleccionado != null && !cedulaPacienteSeleccionado.isEmpty()) {
            newDocumento.setUsuarioSaludCedula(cedulaPacienteSeleccionado);
        }

        // Pre-llenar profesional actual (del SessionBean)
        Integer profesionalCi = sessionBean.getProfesionalCi();
        if (profesionalCi != null) {
            newDocumento.setProfesionalCi(profesionalCi);
        }

        // Fecha de inicio por defecto: hoy
        newDocumento.setFechaInicioDiagnostico(LocalDate.now());
    }

    /**
     * Guarda un nuevo documento clínico
     */
    public void guardarDocumento() {
        try {
            // Validaciones
            if (newDocumento.getUsuarioSaludCedula() == null || newDocumento.getUsuarioSaludCedula().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Debe seleccionar un paciente");
                return;
            }
            if (newDocumento.getProfesionalCi() == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el CI del profesional");
                return;
            }

            // Extraer código del motivo de consulta del autocompletado
            if (motivoConsultaAutoComplete == null || motivoConsultaAutoComplete.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El motivo de consulta es obligatorio");
                return;
            }
            String codigoMotivo = extraerCodigoDeAutoComplete(motivoConsultaAutoComplete);
            if (codigoMotivo == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Formato de motivo de consulta inválido");
                return;
            }
            newDocumento.setCodigoMotivoConsulta(codigoMotivo);
            if (newDocumento.getDescripcionDiagnostico() == null || newDocumento.getDescripcionDiagnostico().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "La descripción del diagnóstico es obligatoria");
                return;
            }
            if (newDocumento.getFechaInicioDiagnostico() == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "La fecha de inicio del diagnóstico es obligatoria");
                return;
            }
            if (newDocumento.getCodigoGradoCerteza() == null || newDocumento.getCodigoGradoCerteza().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El grado de certeza es obligatorio");
                return;
            }

            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                return;
            }

            // Llamar al APIService
            documento_clinico_dto resultado = apiService.crearDocumento(
                newDocumento.getUsuarioSaludCedula(),
                newDocumento.getProfesionalCi(),
                newDocumento.getCodigoMotivoConsulta(),
                newDocumento.getDescripcionDiagnostico(),
                newDocumento.getFechaInicioDiagnostico().toString(),
                newDocumento.getCodigoEstadoProblema(),
                newDocumento.getCodigoGradoCerteza(),
                newDocumento.getFechaProximaConsulta() != null ? newDocumento.getFechaProximaConsulta().toString() : null,
                newDocumento.getDescripcionProximaConsulta(),
                newDocumento.getReferenciaAlta(),
                UUID.fromString(tenantId)
            );

            if (resultado != null) {
                addMessage(FacesMessage.SEVERITY_INFO, "Documento clínico creado exitosamente");
                cargarDocumentosPorPaciente(); // Recargar lista
                newDocumento = new documento_clinico_dto(); // Reset form
                PrimeFaces primeFaces = PrimeFaces.current();
                if (primeFaces != null) {
                    primeFaces.ajax().update("documentosForm");
                    primeFaces.executeScript("PF('dlgNuevoDocumento').hide();");
                }
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Error al crear el documento");
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al guardar documento: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Se ejecuta antes de renderizar la vista para asegurar que las listas estén cargadas
     * cuando el profesional inicia sesión desde la pantalla principal.
     */
    public void prepararVista() {
        if (!sessionBean.isLoggedIn()) {
            return;
        }
        if (pacientes == null || pacientes.isEmpty()) {
            cargarPacientes();
        }
        if (motivosConsulta == null || motivosConsulta.isEmpty()) {
            cargarCatalogos();
        }
    }

    /**
     * Selecciona un documento para ver detalles
     */
    public void seleccionarDocumento(documento_clinico_dto documento) {
        this.selectedDocumento = documento;
    }

    /**
     * Método de autocompletado para motivos de consulta
     * Llamado por p:autoComplete cuando el usuario escribe
     */
    public List<String> completeMotivo(String query) {
        try {
            if (query == null || query.trim().length() < 2) {
                return new ArrayList<>(); // Requiere al menos 2 caracteres
            }

            Map<String, String> resultados = apiService.buscarMotivosConsulta(query.trim());
            List<String> sugerencias = new ArrayList<>();

            for (Map.Entry<String, String> entry : resultados.entrySet()) {
                String codigo = entry.getKey();
                String nombre = entry.getValue();

                // Guardar en cache para poder recuperar el código luego
                motivosCache.put(nombre, codigo);

                // Devolver solo el nombre (no el código)
                sugerencias.add(nombre);
            }

            return sugerencias;
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al buscar motivos: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Convierte el Map de codigueras a una lista de SelectItem para dropdowns
     */
    public List<Map.Entry<String, String>> getMotivosConsultaList() {
        if (motivosConsulta == null) return new ArrayList<>();
        return new ArrayList<>(motivosConsulta.entrySet());
    }

    public List<Map.Entry<String, String>> getEstadosProblemaList() {
        if (estadosProblema == null) return new ArrayList<>();
        return new ArrayList<>(estadosProblema.entrySet());
    }

    public List<Map.Entry<String, String>> getGradosCertezaList() {
        if (gradosCerteza == null) return new ArrayList<>();
        return new ArrayList<>(gradosCerteza.entrySet());
    }

    /**
     * Extrae el código a partir del nombre seleccionado en el autocompletado
     * Busca en el cache de motivos que se llenó durante la búsqueda
     */
    private String extraerCodigoDeAutoComplete(String nombreSeleccionado) {
        if (nombreSeleccionado == null || nombreSeleccionado.trim().isEmpty()) {
            return null;
        }
        // Buscar el código en el cache usando el nombre
        return motivosCache.get(nombreSeleccionado);
    }

    /**
     * Agrega mensaje de FacesContext
     */
    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, message, null));
    }

    // ============ GETTERS Y SETTERS ============

    public List<documento_clinico_dto> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<documento_clinico_dto> documentos) {
        this.documentos = documentos;
    }

    public List<usuario_salud_dto> getPacientes() {
        return pacientes;
    }

    public void setPacientes(List<usuario_salud_dto> pacientes) {
        this.pacientes = pacientes;
    }

    public Map<String, String> getMotivosConsulta() {
        return motivosConsulta;
    }

    public void setMotivosConsulta(Map<String, String> motivosConsulta) {
        this.motivosConsulta = motivosConsulta;
    }

    public Map<String, String> getEstadosProblema() {
        return estadosProblema;
    }

    public void setEstadosProblema(Map<String, String> estadosProblema) {
        this.estadosProblema = estadosProblema;
    }

    public Map<String, String> getGradosCerteza() {
        return gradosCerteza;
    }

    public void setGradosCerteza(Map<String, String> gradosCerteza) {
        this.gradosCerteza = gradosCerteza;
    }

    public documento_clinico_dto getNewDocumento() {
        return newDocumento;
    }

    public void setNewDocumento(documento_clinico_dto newDocumento) {
        this.newDocumento = newDocumento;
    }

    public documento_clinico_dto getSelectedDocumento() {
        return selectedDocumento;
    }

    public void setSelectedDocumento(documento_clinico_dto selectedDocumento) {
        this.selectedDocumento = selectedDocumento;
    }

    public String getCedulaPacienteSeleccionado() {
        return cedulaPacienteSeleccionado;
    }

    public void setCedulaPacienteSeleccionado(String cedulaPacienteSeleccionado) {
        this.cedulaPacienteSeleccionado = cedulaPacienteSeleccionado;
    }

    public usuario_salud_dto getPacienteSeleccionado() {
        return pacienteSeleccionado;
    }

    public void setPacienteSeleccionado(usuario_salud_dto pacienteSeleccionado) {
        this.pacienteSeleccionado = pacienteSeleccionado;
    }

    public String getMotivoConsultaAutoComplete() {
        return motivoConsultaAutoComplete;
    }

    public void setMotivoConsultaAutoComplete(String motivoConsultaAutoComplete) {
        this.motivoConsultaAutoComplete = motivoConsultaAutoComplete;
    }
}
