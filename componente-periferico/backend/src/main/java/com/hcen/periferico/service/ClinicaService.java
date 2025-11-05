package com.hcen.periferico.service;

import com.hcen.periferico.entity.clinica;
import com.hcen.periferico.dao.ClinicaDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.logging.Logger;

@Stateless
public class ClinicaService {

    private static final Logger LOGGER = Logger.getLogger(ClinicaService.class.getName());

    @EJB
    private ClinicaDAO clinicaDAO;

    @EJB
    private AuthenticationService authenticationService;

    public clinica crearClinicaConAdministrador(String nombre, String direccion, String email) {
        clinica nuevaClinica = new clinica();
        nuevaClinica.setNombre(nombre);
        nuevaClinica.setDireccion(direccion);
        nuevaClinica.setEmail(email);
        nuevaClinica.setEstado("ACTIVA");
        nuevaClinica.setFecRegistro(LocalDateTime.now(ZoneId.of("America/Montevideo")));

        clinica almacenada = clinicaDAO.save(nuevaClinica);
        crearAdministradorPorDefecto(almacenada);
        return almacenada;
    }

    private void crearAdministradorPorDefecto(clinica clinica) {
        String clinicName = clinica.getNombre() != null ? clinica.getNombre().trim() : "";
        if (clinicName.isEmpty()) {
            clinicName = "clinica-" + clinica.getTenantId().toString().substring(0, 8);
        }

        String username = generarUsername(clinica, clinicName);
        String nombre = truncate("administrador-" + clinicName, 100);
        String apellidos = truncate(clinicName, 100);

        final String logClinicName = clinicName;
        final String logUsername = username;
        LOGGER.info(() -> "Creando administrador por defecto para clínica " + logClinicName + " con usuario " + logUsername);
        authenticationService.createAdmin(username, "1234", nombre, apellidos, clinica.getTenantId(), false);
        LOGGER.info(() -> "Administrador por defecto creado para clínica " + logClinicName);
    }

    private String generarUsername(clinica clinica, String clinicName) {
        String cleanedName = clinicName;
        if (cleanedName == null || cleanedName.isBlank()) {
            cleanedName = clinica.getTenantId().toString().substring(0, 8);
        }

        return truncate("administrador-" + cleanedName.trim(), 80);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
