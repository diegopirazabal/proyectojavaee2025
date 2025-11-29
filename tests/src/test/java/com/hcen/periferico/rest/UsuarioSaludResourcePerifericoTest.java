package com.hcen.periferico.rest;

import com.hcen.periferico.dto.usuario_salud_dto;
import com.hcen.periferico.enums.TipoDocumento;
import com.hcen.periferico.service.UsuarioSaludService;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UsuarioSaludResourcePerifericoTest {

    private UsuarioSaludResource resource;
    private UsuarioSaludService usuarioService;

    @Before
    public void setUp() throws Exception {
        resource = new UsuarioSaludResource();
        usuarioService = mock(UsuarioSaludService.class);
        inject(resource, "usuarioService", usuarioService);
    }

    @Test
    public void registrarUsuarioExitoso() throws Exception {
        UUID tenant = UUID.randomUUID();
        UsuarioSaludResource.RegistrarUsuarioRequest request = new UsuarioSaludResource.RegistrarUsuarioRequest();
        request.setCedula("12345678");
        request.setTipoDocumento("DO");
        request.setPrimerNombre("Ana");
        request.setPrimerApellido("Lopez");
        request.setEmail("ana@example.com");
        request.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        request.setTenantId(tenant.toString());

        usuario_salud_dto dto = new usuario_salud_dto();
        dto.setCedula("12345678");
        dto.setTenantId(tenant.toString());
        when(usuarioService.registrarUsuarioEnClinica(anyString(), any(), anyString(), any(), anyString(), any(),
            anyString(), any(), eq(tenant))).thenReturn(dto);

        Response response = resource.registrarUsuario(request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertSame(dto, response.getEntity());
        verify(usuarioService).registrarUsuarioEnClinica(
            eq("12345678"), eq(TipoDocumento.DO), eq("Ana"), eq(request.getSegundoNombre()),
            eq("Lopez"), eq(request.getSegundoApellido()), eq("ana@example.com"),
            eq(LocalDate.of(1990, 1, 1)), eq(tenant));
    }

    @Test
    public void registrarUsuarioSinTenantDevuelveBadRequest() {
        UsuarioSaludResource.RegistrarUsuarioRequest request = new UsuarioSaludResource.RegistrarUsuarioRequest();
        request.setCedula("1234");

        Response response = resource.registrarUsuario(request);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(usuarioService);
    }

    @Test
    public void getUsuarioByCedulaNotFound() {
        UUID tenant = UUID.randomUUID();
        when(usuarioService.getUsuarioByCedulaAndTenant("000", tenant)).thenReturn(null);

        Response response = resource.getUsuarioByCedula("000", tenant.toString());

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteUsuarioInvocaServicio() {
        UUID tenant = UUID.randomUUID();

        Response response = resource.deleteUsuario("123", tenant.toString());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(usuarioService).desactivarUsuario("123", tenant);
    }

    @Test
    public void getUsuariosUsaBusquedaCuandoHayTermino() {
        UUID tenant = UUID.randomUUID();
        usuario_salud_dto dto = new usuario_salud_dto();
        dto.setCedula("1");
        when(usuarioService.searchUsuariosByTenantId("ana", tenant)).thenReturn(List.of(dto));

        Response response = resource.getUsuarios(tenant.toString(), "ana");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        List<usuario_salud_dto> data = (List<usuario_salud_dto>) response.getEntity();
        assertEquals(1, data.size());
        verify(usuarioService).searchUsuariosByTenantId("ana", tenant);
        verify(usuarioService, never()).getAllUsuariosByTenantId(any());
    }

    @Test
    public void getUsuariosSinTenantDevuelveBadRequest() {
        Response response = resource.getUsuarios(null, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(usuarioService);
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
