package com.hcen.periferico.rest;

import com.hcen.periferico.entity.configuracion_clinica;
import com.hcen.periferico.service.ConfiguracionService;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ConfiguracionResourceTest {

    private ConfiguracionResource resource;
    private ConfiguracionService configuracionService;

    @Before
    public void setUp() throws Exception {
        resource = new ConfiguracionResource();
        configuracionService = mock(ConfiguracionService.class);
        inject(resource, "configuracionService", configuracionService);
    }

    @Test
    public void updateLookAndFeelRetornaDto() throws Exception {
        UUID tenantId = UUID.randomUUID();
        configuracion_clinica config = buildConfig(tenantId);
        config.setColorPrimario("#000000");
        when(configuracionService.actualizarLookAndFeel(eq(tenantId.toString()), any(), any(), any(), any(), any()))
            .thenReturn(config);

        ConfiguracionResource.LookFeelRequest request = new ConfiguracionResource.LookFeelRequest();
        request.setColorPrimario("#000000");
        request.setColorSecundario("#FFFFFF");
        request.setLogoUrl("http://logo.png");
        request.setNombreSistema("Clinica Test");
        request.setTema("dark");

        Response response = resource.updateLookAndFeel(tenantId.toString(), request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        var dto = (com.hcen.periferico.dto.configuracion_clinica_dto) response.getEntity();
        assertEquals(tenantId, dto.getTenantId());
        assertEquals("#000000", dto.getColorPrimario());
        verify(configuracionService).actualizarLookAndFeel(eq(tenantId.toString()),
            eq("#000000"), eq("#FFFFFF"), eq("http://logo.png"), eq("Clinica Test"), eq("dark"));
    }

    @Test
    public void toggleNodoPerifericoActualizaFlag() throws Exception {
        UUID tenantId = UUID.randomUUID();
        configuracion_clinica config = buildConfig(tenantId);
        config.setNodoPerifericoHabilitado(true);
        when(configuracionService.toggleNodoPeriferico(tenantId.toString(), true)).thenReturn(config);

        ConfiguracionResource.NodoRequest request = new ConfiguracionResource.NodoRequest();
        request.setHabilitado(true);

        Response response = resource.toggleNodoPeriferico(tenantId.toString(), request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        var dto = (com.hcen.periferico.dto.configuracion_clinica_dto) response.getEntity();
        assertTrue(dto.getNodoPerifericoHabilitado());
        verify(configuracionService).toggleNodoPeriferico(tenantId.toString(), true);
    }

    @Test
    public void resetToDefaultPropagaResultado() throws Exception {
        UUID tenantId = UUID.randomUUID();
        configuracion_clinica config = buildConfig(tenantId);
        config.setColorPrimario("#123456");
        when(configuracionService.resetToDefault(tenantId.toString())).thenReturn(config);

        Response response = resource.resetToDefault(tenantId.toString());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        var dto = (com.hcen.periferico.dto.configuracion_clinica_dto) response.getEntity();
        assertEquals("#123456", dto.getColorPrimario());
        verify(configuracionService).resetToDefault(tenantId.toString());
    }

    private static configuracion_clinica buildConfig(UUID tenantId) {
        configuracion_clinica config = new configuracion_clinica();
        config.setTenantId(tenantId);
        config.setColorPrimario("#FFFFFF");
        config.setColorSecundario("#000000");
        config.setTema("default");
        config.setNombreSistema("HCEN");
        config.setNodoPerifericoHabilitado(false);
        return config;
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
