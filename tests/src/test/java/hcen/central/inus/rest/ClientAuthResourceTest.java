package hcen.central.inus.rest;

import hcen.central.inus.dto.ClientAuthRequest;
import hcen.central.inus.dto.ClientAuthResponse;
import hcen.central.inus.service.ClientAuthenticationService;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ClientAuthResource} sin necesidad de contenedor EJB.
 */
public class ClientAuthResourceTest {

    private ClientAuthResource resource;
    private ClientAuthenticationService authenticationService;

    @Before
    public void setUp() throws Exception {
        resource = new ClientAuthResource();
        authenticationService = mock(ClientAuthenticationService.class);
        Field field = ClientAuthResource.class.getDeclaredField("clientAuthService");
        field.setAccessible(true);
        field.set(resource, authenticationService);
    }

    @Test
    public void validaRequestIncompleto() {
        Response response = resource.getToken(new ClientAuthRequest());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(authenticationService);
    }

    @Test
    public void credencialesInvalidasRetornanUnauthorized() {
        ClientAuthRequest request = new ClientAuthRequest();
        request.setClientId("client");
        request.setClientSecret("bad");
        when(authenticationService.authenticateClient("client", "bad")).thenReturn(null);

        Response response = resource.getToken(request);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        verify(authenticationService).authenticateClient("client", "bad");
    }

    @Test
    public void credencialesValidasGeneranToken() {
        ClientAuthRequest request = new ClientAuthRequest();
        request.setClientId("client");
        request.setClientSecret("secret");
        when(authenticationService.authenticateClient("client", "secret")).thenReturn("jwt-token");
        when(authenticationService.getTokenExpirationSeconds()).thenReturn(3600L);

        Response response = resource.getToken(request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        ClientAuthResponse body = (ClientAuthResponse) response.getEntity();
        assertEquals("jwt-token", body.getAccessToken());
        assertEquals(Long.valueOf(3600L), body.getExpiresIn());
        verify(authenticationService).authenticateClient("client", "secret");
        verify(authenticationService).getTokenExpirationSeconds();
    }
}
