package com.hcen.periferico.service;

import com.hcen.periferico.dao.ConfiguracionClinicaDAO;
import com.hcen.periferico.entity.configuracion_clinica;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ConfiguracionServiceTest {

    private ConfiguracionService service;
    private ConfiguracionClinicaDAO configuracionDAO;

    @Before
    public void setUp() throws Exception {
        service = new ConfiguracionService();
        configuracionDAO = mock(ConfiguracionClinicaDAO.class);
        inject(service, "configuracionDAO", configuracionDAO);
    }

    @Test
    public void actualizarLookAndFeelSoloAceptaColoresHex() {
        UUID tenantId = UUID.randomUUID();
        configuracion_clinica config = baseConfig(tenantId);
        when(configuracionDAO.getOrCreateDefault(tenantId)).thenReturn(config);
        when(configuracionDAO.save(any(configuracion_clinica.class))).thenAnswer(invocation -> invocation.getArgument(0));

        configuracion_clinica actualizado = service.actualizarLookAndFeel(
            tenantId.toString(),
            "rojo",              // inválido, no debería pisar
            "#ABCDEF",           // válido
            "http://logo.png",
            "Nombre Nuevo",
            "dark"
        );

        assertEquals("#007bff", actualizado.getColorPrimario());
        assertEquals("#ABCDEF", actualizado.getColorSecundario());
        assertEquals("Nombre Nuevo", actualizado.getNombreSistema());
        verify(configuracionDAO).save(config);
    }

    @Test
    public void resetToDefaultCuandoExisteConfigLaResetea() {
        UUID tenantId = UUID.randomUUID();
        configuracion_clinica config = baseConfig(tenantId);
        config.setColorPrimario("#123456");
        when(configuracionDAO.findByTenantId(tenantId)).thenReturn(Optional.of(config));
        when(configuracionDAO.save(any(configuracion_clinica.class))).thenAnswer(invocation -> invocation.getArgument(0));

        configuracion_clinica reseteada = service.resetToDefault(tenantId.toString());

        assertEquals("#007bff", reseteada.getColorPrimario());
        assertEquals("#6c757d", reseteada.getColorSecundario());
        assertEquals("Sistema de Gestión Clínica", reseteada.getNombreSistema());
        assertFalse(reseteada.getNodoPerifericoHabilitado());
        verify(configuracionDAO).save(config);
    }

    @Test
    public void resetToDefaultCreaSiNoExisteConfig() {
        UUID tenantId = UUID.randomUUID();
        configuracion_clinica creado = baseConfig(tenantId);
        when(configuracionDAO.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(configuracionDAO.getOrCreateDefault(tenantId)).thenReturn(creado);

        configuracion_clinica res = service.resetToDefault(tenantId.toString());

        assertEquals(creado, res);
        verify(configuracionDAO).getOrCreateDefault(tenantId);
    }

    private static configuracion_clinica baseConfig(UUID tenantId) {
        configuracion_clinica c = new configuracion_clinica();
        c.setTenantId(tenantId);
        c.setColorPrimario("#007bff");
        c.setColorSecundario("#6c757d");
        c.setNombreSistema("Sistema de Gestión Clínica");
        c.setNodoPerifericoHabilitado(false);
        return c;
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
