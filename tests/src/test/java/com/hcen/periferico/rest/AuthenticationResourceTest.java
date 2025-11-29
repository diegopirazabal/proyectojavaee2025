package com.hcen.periferico.rest;

import com.hcen.periferico.entity.administrador_clinica;
import com.hcen.periferico.entity.profesional_salud;
import com.hcen.periferico.service.AuthenticationService;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthenticationResourceTest {

    private AuthenticationResource resource;
    private AuthenticationService authService;

    @Before
    public void setUp() throws Exception {
        resource = new AuthenticationResource();
        authService = mock(AuthenticationService.class);
        inject(resource, "authService", authService);
    }

    @Test
    public void loginAdminExitoso() throws Exception {
        UUID tenantId = UUID.randomUUID();
        administrador_clinica admin = new administrador_clinica("admin", "pwd", "Ana", "Lopez", tenantId);
        when(authService.authenticate("admin", "pwd", tenantId)).thenReturn(admin);

        AuthenticationResource.LoginRequest request = new AuthenticationResource.LoginRequest();
        request.setUsername("admin");
        request.setPassword("pwd");
        request.setTenantId(tenantId.toString());

        Response response = resource.login(request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        var dto = (com.hcen.periferico.dto.administrador_clinica_dto) response.getEntity();
        assertEquals("admin", dto.getUsername());
        assertEquals("Ana", dto.getNombre());
        verify(authService).authenticate("admin", "pwd", tenantId);
    }

    @Test
    public void loginAdminCredencialesInvalidas() {
        UUID tenantId = UUID.randomUUID();
        AuthenticationResource.LoginRequest request = new AuthenticationResource.LoginRequest();
        request.setUsername("admin");
        request.setPassword("bad");
        request.setTenantId(tenantId.toString());
        when(authService.authenticate("admin", "bad", tenantId)).thenReturn(null);

        Response response = resource.login(request);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void loginAdminTenantInvalidoDevuelveBadRequest() {
        AuthenticationResource.LoginRequest request = new AuthenticationResource.LoginRequest();
        request.setUsername("admin");
        request.setPassword("pwd");
        request.setTenantId("no-uuid");

        Response response = resource.login(request);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(authService);
    }

    @Test
    public void loginProfesionalExitoso() throws Exception {
        UUID tenantId = UUID.randomUUID();
        profesional_salud profesional = new profesional_salud();
        profesional.setEmail("pro@demo.com");
        when(authService.authenticateProfesional("pro@demo.com", "pwd", tenantId)).thenReturn(profesional);

        AuthenticationResource.ProfesionalLoginRequest request = new AuthenticationResource.ProfesionalLoginRequest();
        request.setEmail("pro@demo.com");
        request.setPassword("pwd");
        request.setTenantId(tenantId.toString());

        Response response = resource.loginProfesional(request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof com.hcen.periferico.dto.profesional_salud_dto);
        verify(authService).authenticateProfesional("pro@demo.com", "pwd", tenantId);
    }

    @Test
    public void loginProfesionalConRequestIncompletoDevuelveBadRequest() {
        Response response = resource.loginProfesional(new AuthenticationResource.ProfesionalLoginRequest());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(authService);
    }

    @Test
    public void loginProfesionalCredencialesInvalidas() {
        UUID tenantId = UUID.randomUUID();
        AuthenticationResource.ProfesionalLoginRequest request = new AuthenticationResource.ProfesionalLoginRequest();
        request.setEmail("pro@demo.com");
        request.setPassword("bad");
        request.setTenantId(tenantId.toString());
        when(authService.authenticateProfesional("pro@demo.com", "bad", tenantId)).thenReturn(null);

        Response response = resource.loginProfesional(request);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
