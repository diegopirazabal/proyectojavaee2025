package hcen.central.inus.rest;

import hcen.central.inus.dto.OIDCAuthRequest;
import hcen.central.inus.security.oidc.OIDCAuthenticationService;
import java.lang.reflect.Field;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias básicas para {@link OIDCAuthResource#login(String, String)}.
 */
public class OIDCAuthResourceTest {

    private OIDCAuthResource resource;
    private OIDCAuthenticationService authService;
    private HttpServletRequest httpRequest;
    private HttpSession session;

    @Before
    public void setUp() throws Exception {
        resource = new OIDCAuthResource();
        authService = mock(OIDCAuthenticationService.class);
        httpRequest = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);

        Field authField = OIDCAuthResource.class.getDeclaredField("authService");
        authField.setAccessible(true);
        authField.set(resource, authService);

        Field requestField = OIDCAuthResource.class.getDeclaredField("httpRequest");
        requestField.setAccessible(true);
        requestField.set(resource, httpRequest);
    }

    @Test
    public void loginSinRedirectUriDevuelveBadRequest() {
        when(httpRequest.getHeader("Referer")).thenReturn(null);

        Response response = resource.login(null, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(authService);
    }

    @Test
    public void loginMobileUsaRedirectDelBackend() {
        when(httpRequest.getServerName()).thenReturn("localhost");
        when(httpRequest.getSession(true)).thenReturn(session);

        OIDCAuthRequest authRequest = new OIDCAuthRequest();
        authRequest.setAuthorizationUrl("https://oidc/authorize");
        authRequest.setState("state123");
        authRequest.setNonce("nonce123");

        when(authService.initiateLogin(eq("http://localhost:8080/hcen-central/api/auth/callback")))
            .thenReturn(authRequest);

        Response response = resource.login(null, "mobile");

        assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
        assertTrue(response.getLocation().toString().contains("https://oidc/authorize"));
        verify(session).setAttribute("oidc_state", "state123");
        verify(session).setAttribute("oidc_nonce", "nonce123");
    }
}
