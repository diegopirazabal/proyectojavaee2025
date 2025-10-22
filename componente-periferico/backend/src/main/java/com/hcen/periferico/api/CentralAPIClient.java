package com.hcen.periferico.api;

import com.hcen.periferico.dto.usuario_salud_dto;
import com.hcen.periferico.enums.TipoDocumento;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente REST para comunicarse con el componente central (INUS)
 * Maneja todas las operaciones relacionadas con usuarios de salud
 */
@Stateless
public class CentralAPIClient {

    private static final Logger LOGGER = Logger.getLogger(CentralAPIClient.class.getName());

    // URL base del componente central - TODO: Hacer configurable via properties/env var
    private static final String CENTRAL_BASE_URL = System.getenv("HCEN_CENTRAL_URL") != null
        ? System.getenv("HCEN_CENTRAL_URL")
        : "http://localhost:8080";

    private static final String API_USUARIOS = CENTRAL_BASE_URL + "/api/usuarios";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;

    public CentralAPIClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    }

    /**
     * Verifica si un usuario existe en el componente central por cédula
     */
    public boolean verificarUsuarioExiste(String cedula) {
        try {
            String url = API_USUARIOS + "/verificar/" + cedula;
            LOGGER.info("Verificando existencia de usuario en central: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonReader jsonReader = Json.createReader(new StringReader(response.body()));
                JsonObject jsonObject = jsonReader.readObject();
                boolean existe = jsonObject.getBoolean("existe", false);
                LOGGER.info("Usuario " + cedula + " existe en central: " + existe);
                return existe;
            } else {
                LOGGER.warning("Error al verificar usuario. Status: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al verificar usuario en central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Registra un usuario en una clínica en el componente central
     */
    public usuario_salud_dto registrarUsuarioEnClinica(String cedula, TipoDocumento tipoDocumento,
                                                       String primerNombre, String segundoNombre,
                                                       String primerApellido, String segundoApellido,
                                                       String email, LocalDate fechaNacimiento,
                                                       String clinicaRut) {
        try {
            String url = API_USUARIOS + "/registrar";
            LOGGER.info("=== Registrando usuario en central ===");
            LOGGER.info("URL completa: " + url);
            LOGGER.info("CENTRAL_BASE_URL: " + CENTRAL_BASE_URL);

            // Construir JSON del request
            var jsonBuilder = Json.createObjectBuilder()
                .add("cedula", cedula)
                .add("tipoDocumento", tipoDocumento.name())
                .add("primerNombre", primerNombre)
                .add("primerApellido", primerApellido)
                .add("email", email)
                .add("fechaNacimiento", fechaNacimiento.toString())
                .add("clinicaRut", clinicaRut);

            // Agregar campos opcionales
            if (segundoNombre != null && !segundoNombre.isEmpty()) {
                jsonBuilder.add("segundoNombre", segundoNombre);
            }
            if (segundoApellido != null && !segundoApellido.isEmpty()) {
                jsonBuilder.add("segundoApellido", segundoApellido);
            }

            String jsonBody = jsonBuilder.build().toString();
            LOGGER.fine("Request body: " + jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOGGER.info("Usuario registrado exitosamente en central");
                return parseUsuarioFromJson(response.body());
            } else {
                String errorMsg = "Error al registrar usuario. Status: " + response.statusCode() +
                                ", Body: " + response.body();
                LOGGER.severe(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar usuario en central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene datos de un usuario por cédula desde el componente central
     */
    public usuario_salud_dto getUsuarioByCedula(String cedula) {
        try {
            String url = API_USUARIOS + "/" + cedula;
            LOGGER.info("Obteniendo usuario desde central: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseUsuarioFromJson(response.body());
            } else if (response.statusCode() == 404) {
                LOGGER.info("Usuario no encontrado en central: " + cedula);
                return null;
            } else {
                LOGGER.warning("Error al obtener usuario. Status: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuario desde central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Desasocia un usuario de una clínica en el componente central
     */
    public boolean deleteUsuarioDeClinica(String cedula, String clinicaRut) {
        try {
            String url = API_USUARIOS + "/" + cedula + "/clinica/" + clinicaRut;
            LOGGER.info("Desasociando usuario de clínica en central: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .DELETE()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOGGER.info("Usuario desasociado exitosamente de la clínica");
                return true;
            } else if (response.statusCode() == 404) {
                LOGGER.warning("No se encontró la asociación usuario-clínica");
                return false;
            } else {
                String errorMsg = "Error al desasociar usuario. Status: " + response.statusCode() +
                                ", Body: " + response.body();
                LOGGER.severe(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error al desasociar usuario en central", e);
            throw new RuntimeException("Error al comunicarse con el componente central: " + e.getMessage(), e);
        }
    }

    /**
     * Parsea la respuesta JSON del central a DTO
     */
    private usuario_salud_dto parseUsuarioFromJson(String jsonString) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = reader.readObject();

            usuario_salud_dto dto = new usuario_salud_dto();
            dto.setCedula(jsonObject.getString("cedula"));
            dto.setPrimerNombre(jsonObject.getString("primerNombre"));
            dto.setSegundoNombre(jsonObject.getString("segundoNombre", null));
            dto.setPrimerApellido(jsonObject.getString("primerApellido"));
            dto.setSegundoApellido(jsonObject.getString("segundoApellido", null));
            dto.setEmail(jsonObject.getString("email"));

            String tipoDocStr = jsonObject.getString("tipoDocumento", "CI");
            dto.setTipoDocumento(TipoDocumento.valueOf(tipoDocStr));

            String fechaNacStr = jsonObject.getString("fechaNacimiento", null);
            if (fechaNacStr != null) {
                dto.setFechaNacimiento(LocalDate.parse(fechaNacStr));
            }

            return dto;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al parsear JSON de usuario", e);
            throw new RuntimeException("Error al procesar respuesta del componente central", e);
        }
    }
}
