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

/**
 * Managed Bean para gestiÃ³n de documentos clÃ­nicos ambulatorios.
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

    // CatÃ¡logos (codigueras)
    private Map<String, String> motivosConsulta;
    private Map<String, String> estadosProblema;
    private Map<String, String> gradosCerteza;

    // Documento en ediciÃ³n
    private documento_clinico_dto newDocumento;
    private documento_clinico_dto selectedDocumento;

    // Filtros
    private String cedulaPacienteSeleccionado;
    private usuario_salud_dto pacienteSeleccionado;

    // Autocompletado
    private String motivoConsultaAutoComplete; // Para el componente p:autoComplete (solo el nombre)
    private Map<String, String> motivosCache; // Cache de nombre -> cÃ³digo para autocompletado

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
     * Carga los catÃ¡logos de codigueras desde el backend
     */
    public void cargarCatalogos() {
        try {
            motivosConsulta = apiService.getMotivosConsulta();
            estadosProblema = apiService.getEstadosProblema();
            gradosCerteza = apiService.getGradosCerteza();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al cargar catÃ¡logos: " + e.getMessage());
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
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clÃ­nica");
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
     * Carga documentos de un paciente especÃ­fico
     */
    public void cargarDocumentosPorPaciente() {
        if (cedulaPacienteSeleccionado == null || cedulaPacienteSeleccionado.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Debe seleccionar un paciente");
            return;
        }

        try {
            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clÃ­nica");
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
    public String prepararNuevoDocumento() {
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
        return "/pages/documentos/nuevo.xhtml?faces-redirect=true";
    }

    /**
     * Guarda un nuevo documento clÃ­nico
     */
    public String guardarDocumento() {
        try {
            if (newDocumento.getUsuarioSaludCedula() == null || newDocumento.getUsuarioSaludCedula().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Debe seleccionar un paciente");
                return null;
            }
            if (newDocumento.getProfesionalCi() == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el CI del profesional");
                return null;
            }

            if (motivoConsultaAutoComplete == null || motivoConsultaAutoComplete.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El motivo de consulta es obligatorio");
                return null;
            }
            String codigoMotivo = extraerCodigoDeAutoComplete(motivoConsultaAutoComplete);
            if (codigoMotivo == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Formato de motivo de consulta inválido");
                return null;
            }
            newDocumento.setCodigoMotivoConsulta(codigoMotivo);

            if (newDocumento.getDescripcionDiagnostico() == null || newDocumento.getDescripcionDiagnostico().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "La descripción del diagnóstico es obligatoria");
                return null;
            }
            if (newDocumento.getFechaInicioDiagnostico() == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "La fecha de inicio del diagnóstico es obligatoria");
                return null;
            }
            if (newDocumento.getCodigoGradoCerteza() == null || newDocumento.getCodigoGradoCerteza().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "El grado de certeza es obligatorio");
                return null;
            }

            String tenantId = sessionBean.getTenantId();
            if (tenantId == null || tenantId.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo obtener el ID de la clínica");
                return null;
            }

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
                cargarDocumentosPorPaciente();
                newDocumento = new documento_clinico_dto();
                return "/pages/documentos.xhtml?faces-redirect=true";
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Error al crear el documento");
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al guardar documento: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * MÃ©todo provisional para forzar sincronizaciÃ³n manual con el componente central
     * Ãštil para debugging y testing sin tener que crear nuevos documentos
     */
    public void forzarSincronizacion() {
        try {
            System.out.println("=== Forzando sincronizaciÃ³n manual desde frontend ===");
            boolean exito = apiService.sincronizarPendientes();

            if (exito) {
                addMessage(FacesMessage.SEVERITY_INFO, "SincronizaciÃ³n iniciada correctamente. Revisa los logs del servidor para ver el resultado.");
            } else {
                addMessage(FacesMessage.SEVERITY_WARN, "La sincronizaciÃ³n se ejecutÃ³ pero hubo problemas. Revisa los logs del servidor.");
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error al forzar sincronizaciÃ³n: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Se ejecuta antes de renderizar la vista para asegurar que las listas estÃ©n cargadas
     * cuando el profesional inicia sesiÃ³n desde la pantalla principal.
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
     * Selecciona un documento para ver detalles y navega a la pÃ¡gina de visualizaciÃ³n.
     */
    public String seleccionarDocumento(documento_clinico_dto documento) {
        if (documento == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "No se pudo cargar el documento seleccionado");
            return null;
        }
        this.selectedDocumento = documento;
        return "/pages/documentos/ver.xhtml?faces-redirect=true";
    }

    /**
     * MÃ©todo de autocompletado para motivos de consulta
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

                // Guardar en cache para poder recuperar el cÃ³digo luego
                motivosCache.put(nombre, codigo);

                // Devolver solo el nombre (no el cÃ³digo)
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
     * Extrae el cÃ³digo a partir del nombre seleccionado en el autocompletado
     * Busca en el cache de motivos que se llenÃ³ durante la bÃºsqueda
     */
    private String extraerCodigoDeAutoComplete(String nombreSeleccionado) {
        if (nombreSeleccionado == null || nombreSeleccionado.trim().isEmpty()) {
            return null;
        }
        // Buscar el cÃ³digo en el cache usando el nombre
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

