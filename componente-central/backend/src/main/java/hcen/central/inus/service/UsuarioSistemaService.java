package hcen.central.inus.service;

import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.UsuarioSistemaResponse;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.enums.UsuarioSistemaTipo;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class UsuarioSistemaService {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSistemaService.class.getName());
    private static final int DEFAULT_LIMIT = 150;
    private static final int MAX_LIMIT = 500;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

    @EJB
    private PerifericoUsuariosClient perifericoUsuariosClient;

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

    public List<UsuarioSistemaResponse> obtenerCatalogo(String tipoDocumento,
                                                        String numeroDocumento,
                                                        String nombre,
                                                        String apellido,
                                                        Integer limit) {
        int resolvedLimit = normalizeLimit(limit);
        TipoDocumento tipoDocEnum = parseTipoDocumento(tipoDocumento);

        if (tipoDocumento != null && !tipoDocumento.isBlank() && tipoDocEnum == null) {
            throw new IllegalArgumentException("Tipo de documento no soportado: " + tipoDocumento);
        }

        boolean filtroDocumento = tipoDocEnum != null && numeroDocumento != null && !numeroDocumento.isBlank();

        CompletableFuture<List<UsuarioSistemaResponse>> usuariosSaludFuture = CompletableFuture.supplyAsync(() ->
            mapearUsuariosSalud(usuarioSaludDAO.findByFilters(
                tipoDocEnum,
                numeroDocumento,
                nombre,
                apellido,
                0,
                resolvedLimit
            )), executor()
        );

        CompletableFuture<List<UsuarioSistemaResponse>> profesionalesFuture = filtroDocumento && !puedeBuscarProfesionalPorDocumento(tipoDocEnum)
            ? CompletableFuture.completedFuture(Collections.emptyList())
            : CompletableFuture.supplyAsync(() ->
                perifericoUsuariosClient.listarProfesionales(
                    numeroDocumento,
                    tipoDocEnum,
                    nombre,
                    apellido,
                    resolvedLimit
                ), executor()
            );

        CompletableFuture<List<UsuarioSistemaResponse>> administradoresFuture = filtroDocumento
            ? CompletableFuture.completedFuture(Collections.emptyList())
            : CompletableFuture.supplyAsync(() ->
                perifericoUsuariosClient.listarAdministradores(
                    nombre,
                    apellido,
                    resolvedLimit
                ), executor()
            );

        CompletableFuture<List<UsuarioSistemaResponse>> combinados = usuariosSaludFuture
            .thenCombine(profesionalesFuture, (salud, profesionales) -> {
                List<UsuarioSistemaResponse> combined = new ArrayList<>(salud.size() + profesionales.size());
                combined.addAll(salud);
                combined.addAll(profesionales);
                return combined;
            })
            .thenCombine(administradoresFuture, (previo, administradores) -> {
                previo.addAll(administradores);
                return previo;
            })
            .exceptionally(ex -> {
                Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                LOGGER.log(Level.SEVERE, "Error consolidando catálogo de usuarios", cause);
                return Collections.emptyList();
            });

        try {
            return combinados.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Timeout al obtener catálogo de usuarios", e);
            return Collections.emptyList();
        }
    }

    private List<UsuarioSistemaResponse> mapearUsuariosSalud(List<UsuarioSalud> usuarios) {
        List<UsuarioSistemaResponse> respuesta = new ArrayList<>(usuarios.size());
        for (UsuarioSalud usuario : usuarios) {
            UsuarioSistemaResponse dto = new UsuarioSistemaResponse();
            dto.setTipo(UsuarioSistemaTipo.USUARIO_SALUD);
            dto.setOrigen("CENTRAL");
            dto.setId(usuario.getId() != null ? usuario.getId().toString() : usuario.getCedula());
            dto.setNumeroDocumento(usuario.getCedula());
            dto.setTipoDocumento(usuario.getTipoDeDocumento() != null ? usuario.getTipoDeDocumento().name() : null);
            dto.setPrimerNombre(usuario.getPrimerNombre());
            dto.setSegundoNombre(usuario.getSegundoNombre());
            dto.setPrimerApellido(usuario.getPrimerApellido());
            dto.setSegundoApellido(usuario.getSegundoApellido());
            dto.setNombreCompleto(resolverNombreCompleto(usuario));
            dto.setEmail(usuario.getEmail());
            dto.setActivo(usuario.getActive());
            if (usuario.getFechaNacimiento() != null) {
                dto.setFechaNacimiento(DATE_FORMAT.format(usuario.getFechaNacimiento()));
            }
            respuesta.add(dto);
        }
        return respuesta;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private TipoDocumento parseTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            return null;
        }
        try {
            return TipoDocumento.valueOf(tipoDocumento.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Tipo de documento desconocido: {0}", tipoDocumento);
            return null;
        }
    }

    private boolean puedeBuscarProfesionalPorDocumento(TipoDocumento tipoDocumento) {
        return tipoDocumento == null || tipoDocumento == TipoDocumento.DO;
    }

    private java.util.concurrent.Executor executor() {
        return executorService != null ? executorService : Runnable::run;
    }

    private String resolverNombreCompleto(UsuarioSalud usuario) {
        String nombreCompleto = usuario.getNombreCompleto();
        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            return nombreCompleto;
        }
        StringBuilder builder = new StringBuilder();
        if (usuario.getPrimerNombre() != null) {
            builder.append(usuario.getPrimerNombre());
        }
        if (usuario.getSegundoNombre() != null && !usuario.getSegundoNombre().isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(usuario.getSegundoNombre());
        }
        if (usuario.getPrimerApellido() != null) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(usuario.getPrimerApellido());
        }
        if (usuario.getSegundoApellido() != null && !usuario.getSegundoApellido().isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(usuario.getSegundoApellido());
        }
        return builder.toString();
    }
}
