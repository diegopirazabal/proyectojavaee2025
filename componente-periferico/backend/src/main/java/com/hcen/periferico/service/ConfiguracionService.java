package com.hcen.periferico.service;

import com.hcen.core.domain.configuracion_clinica;
import com.hcen.periferico.dao.ConfiguracionClinicaDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.Optional;
import java.util.UUID;

@Stateless
public class ConfiguracionService {

    @EJB
    private ConfiguracionClinicaDAO configuracionDAO;

    /**
     * Obtiene la configuración de una clínica, creándola si no existe
     */
    public configuracion_clinica getConfiguracion(String tenantIdStr) {
        UUID tenantId = UUID.fromString(tenantIdStr);
        return configuracionDAO.getOrCreateDefault(tenantId);
    }

    /**
     * Actualiza la configuración de look & feel de una clínica
     */
    public configuracion_clinica actualizarLookAndFeel(String tenantIdStr, String colorPrimario,
                                                      String colorSecundario, String logoUrl,
                                                      String nombreSistema, String tema) {
        configuracion_clinica config = getConfiguracion(tenantIdStr);

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
    public configuracion_clinica toggleNodoPeriferico(String tenantIdStr, boolean habilitado) {
        configuracion_clinica config = getConfiguracion(tenantIdStr);
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
    public configuracion_clinica resetToDefault(String tenantIdStr) {
        UUID tenantId = UUID.fromString(tenantIdStr);
        Optional<configuracion_clinica> existing = configuracionDAO.findByTenantId(tenantId);
        if (existing.isPresent()) {
            configuracion_clinica config = existing.get();
            config.setColorPrimario("#007bff");
            config.setColorSecundario("#6c757d");
            config.setLogoUrl(null);
            config.setNombreSistema("Sistema de Gestión Clínica");
            config.setTema("default");
            config.setNodoPerifericoHabilitado(false);
            return configuracionDAO.save(config);
        }
        return configuracionDAO.getOrCreateDefault(tenantId);
    }
}
