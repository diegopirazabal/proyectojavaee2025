package hcen.central.inus.rest;

import hcen.central.inus.dto.AdminLoginRequest;
import hcen.central.inus.entity.admin_hcen;
import hcen.central.inus.service.authentication_service;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import jakarta.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link AdminAuthResource}.
 */
public class AdminAuthResourceTest {

    private AdminAuthResource resource;
    private authentication_service authenticationService;

    @Before
    public void setUp() throws Exception {
        resource = new AdminAuthResource();
        authenticationService = mock(authentication_service.class);
        Field field = AdminAuthResource.class.getDeclaredField("authenticationService");
        field.setAccessible(true);
        field.set(resource, authenticationService);
    }

    @Test
    public void validaCredencialesRequeridas() {
        Response response = resource.login(new AdminLoginRequest());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(authenticationService);
    }

    @Test
    public void credencialesInvalidasRetornanUnauthorized() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");
        when(authenticationService.authenticate("admin", "wrong")).thenReturn(null);

        Response response = resource.login(request);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        verify(authenticationService).authenticate("admin", "wrong");
    }

    @Test
    public void loginExitosoDevuelveDatosDelAdmin() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("secret");

        admin_hcen admin = new admin_hcen();
        admin.setId(10L);
        admin.setUsername("admin");
        admin.setFirstName("Mauro");
        admin.setLastName("Gomez");
        admin.setEmail("admin@example.com");
        admin.setActive(true);
        admin.setCreatedAt(LocalDateTime.of(2023, 1, 5, 10, 15));
        admin.setLastLogin(LocalDateTime.of(2023, 1, 6, 9, 0));

        when(authenticationService.authenticate("admin", "secret")).thenReturn(admin);

        Response response = resource.login(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonObject body = (JsonObject) response.getEntity();
        assertEquals("admin", body.getString("username"));
        assertEquals("Mauro", body.getString("firstName"));
        assertEquals("Gomez", body.getString("lastName"));
        assertEquals("admin@example.com", body.getString("email"));
        assertTrue(body.getBoolean("active"));
        verify(authenticationService).authenticate("admin", "secret");
    }
}
