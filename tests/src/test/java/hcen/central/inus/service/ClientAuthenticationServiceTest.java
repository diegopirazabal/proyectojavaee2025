package hcen.central.inus.service;

import hcen.central.inus.dao.JWTSessionDAO;
import hcen.central.inus.entity.JWTSession;
import hcen.central.inus.security.jwt.JWTConfiguration;
import hcen.central.inus.security.jwt.JWTTokenProvider;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ClientAuthenticationServiceTest {

    private ClientAuthenticationService service;
    private JWTTokenProvider tokenProvider;
    private JWTSessionDAO sessionDAO;
    private JWTConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        service = new ClientAuthenticationService();
        tokenProvider = mock(JWTTokenProvider.class);
        sessionDAO = mock(JWTSessionDAO.class);
        configuration = mock(JWTConfiguration.class);
        inject("jwtTokenProvider", tokenProvider);
        inject("jwtSessionDAO", sessionDAO);
        inject("jwtConfig", configuration);
        when(configuration.getJwtAccessTokenExpiration()).thenReturn(60000L);
    }

    @Test
    public void authenticateClientReutilizaSesionActiva() {
        JWTSession session = new JWTSession("componente-periferico", "token-existente",
            LocalDateTime.now().plusMinutes(5));
        setId(session, 10L);
        when(sessionDAO.findActiveByClientId("componente-periferico")).thenReturn(Collections.singletonList(session));

        String token = service.authenticateClient("componente-periferico", "hcen2025_periferico_secret_key");

        assertEquals("token-existente", token);
        verify(sessionDAO).updateLastUsed(10L);
        verify(tokenProvider, never()).generateAccessToken(anyString(), anyString(), anyList());
    }

    @Test
    public void authenticateClientGeneraNuevaSesion() {
        when(sessionDAO.findActiveByClientId("componente-periferico")).thenReturn(Collections.emptyList());
        when(tokenProvider.generateAccessToken(anyString(), anyString(), anyList())).thenReturn("nuevo-token");

        String token = service.authenticateClient("componente-periferico", "hcen2025_periferico_secret_key");

        assertEquals("nuevo-token", token);
        verify(sessionDAO).save(argThat(saved -> saved.getJwtToken().equals("nuevo-token")
            && saved.getExpiresAt().isAfter(LocalDateTime.now())));
    }

    @Test
    public void validateTokenActualizaUltimoUso() {
        JWTSession session = new JWTSession("componente-periferico", "jwt", LocalDateTime.now().plusMinutes(1));
        setId(session, 2L);
        when(sessionDAO.findByToken("jwt")).thenReturn(Optional.of(session));

        assertTrue(service.validateToken("jwt"));
        verify(sessionDAO).updateLastUsed(2L);
    }

    @Test
    public void validateTokenFallaParaExpirados() {
        JWTSession session = new JWTSession("componente", "jwt", LocalDateTime.now().minusMinutes(1));
        when(sessionDAO.findByToken("jwt")).thenReturn(Optional.of(session));

        assertFalse(service.validateToken("jwt"));
    }

    @Test
    public void authenticateClientRechazaCredencialesInvalidas() {
        String token = service.authenticateClient("otro", "secret");
        assertNull(token);
    }

    private void inject(String field, Object value) throws Exception {
        Field f = ClientAuthenticationService.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(service, value);
    }

    private void setId(JWTSession session, Long id) {
        try {
            Field f = JWTSession.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(session, id);
        } catch (Exception ignored) {
        }
    }
}
