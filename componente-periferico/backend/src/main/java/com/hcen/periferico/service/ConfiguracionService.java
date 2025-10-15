package com.hcen.periferico.service;

import com.hcen.core.domain.ConfiguracionClinica;
import com.hcen.periferico.dao.ConfiguracionClinicaDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.Optional;

@Stateless
public class ConfiguracionService {

    @EJB
    private ConfiguracionClinicaDAO configuracionDAO;

    /**
     * Obtiene la configuración de una clínica, creándola si no existe
     */
    public ConfiguracionClinica getConfiguracion(String clinicaRut) {
        return configuracionDAO.getOrCreateDefault(clinicaRut);
    }

    /**
     * Actualiza la configuración de look & feel de una clínica
     */
    public ConfiguracionClinica actualizarLookAndFeel(String clinicaRut, String colorPrimario,
                                                      String colorSecundario, String logoUrl,
                                                      String nombreSistema, String tema) {
        ConfiguracionClinica config = getConfiguracion(clinicaRut);

        if (colorPrimario != null && isValidColor(colorPrimario)) {
            config.setColorPrimario(colorPrimario);
        }
        if (colorSecundario != null && isValidColor(colorSecundario)) {
            config.setColorSecundario(colorSecundario);
        }
        if (logoUrl != null) {
            config.setLogoUrl(logoUrl);
        }
        if (nombreSistema != null && !nombreSistema.trim().isEmpty()) {
            config.setNombreSistema(nombreSistema.trim());
        }
        if (tema != null && !tema.trim().isEmpty()) {
            config.setTema(tema.trim());
        }

        return configuracionDAO.save(config);
    }

    /**
     * Habilita o deshabilita la conexión como nodo periférico
     */
    public ConfiguracionClinica toggleNodoPeriferico(String clinicaRut, boolean habilitado) {
        ConfiguracionClinica config = getConfiguracion(clinicaRut);
        config.setNodoPerifericoHabilitado(habilitado);
        return configuracionDAO.save(config);
    }

    /**
     * Valida que un color esté en formato hexadecimal (#RRGGBB)
     */
    private boolean isValidColor(String color) {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$");
    }

    /**
     * Resetea la configuración a valores por defecto
     */
    public ConfiguracionClinica resetToDefault(String clinicaRut) {
        Optional<ConfiguracionClinica> existing = configuracionDAO.findByClinicaRut(clinicaRut);
        if (existing.isPresent()) {
            ConfiguracionClinica config = existing.get();
            config.setColorPrimario("#007bff");
            config.setColorSecundario("#6c757d");
            config.setLogoUrl(null);
            config.setNombreSistema("Sistema de Gestión Clínica");
            config.setTema("default");
            config.setNodoPerifericoHabilitado(false);
            return configuracionDAO.save(config);
        }
        return configuracionDAO.getOrCreateDefault(clinicaRut);
    }
}
