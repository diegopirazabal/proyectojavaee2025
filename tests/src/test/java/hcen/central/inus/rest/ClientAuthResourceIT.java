package hcen.central.inus.rest;

import hcen.central.inus.dao.JWTSessionDAO;
import hcen.central.inus.dto.ClientAuthRequest;
import hcen.central.inus.dto.ClientAuthResponse;
import hcen.central.inus.entity.JWTSession;
import hcen.central.inus.security.exceptions.InvalidTokenException;
import hcen.central.inus.security.jwt.JWTConfiguration;
import hcen.central.inus.security.jwt.JWTTokenProvider;
import hcen.central.inus.service.ClientAuthenticationService;
import hcen.central.inus.testsupport.ArquillianMavenResolver;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.arquillian.container.test.api.Deployment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pruebas de integración para {@link ClientAuthResource} validando el flujo
 * completo de autenticación de clientes y persistencia de sesiones JWT.
 */

@RunWith(Arquillian.class)
public class ClientAuthResourceIT {

    private static final String CLIENT_ID = "componente-periferico";

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve("com.h2database:h2");

        return ShrinkWrap.create(WebArchive.class, "client-auth-resource-it.war")
                .addClasses(
                        ClientAuthResource.class,
                        ClientAuthenticationService.class,
                        ClientAuthRequest.class,
                        ClientAuthResponse.class,
                        JWTSessionDAO.class,
                        JWTSession.class,
                        JWTTokenProvider.class,
                        JWTConfiguration.class,
                        InvalidTokenException.class,
                        hcen.central.inus.testsupport.converter.TestInstantAttributeConverter.class,
                        hcen.central.inus.testsupport.converter.TestUUIDAttributeConverter.class
                )
                .addAsLibraries(libs)
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("META-INF/oidc-config.properties", "META-INF/oidc-config.properties")
                .addAsWebInfResource("test-ds/resources.xml", "resources.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private ClientAuthResource clientAuthResource;

    @EJB
    private JWTSessionDAO jwtSessionDAO;

    @Before
    public void cleanSessions() {
        List<JWTSession> sessions = jwtSessionDAO.findAllActive();
        for (JWTSession session : sessions) {
            jwtSessionDAO.invalidateByToken(session.getJwtToken());
        }
    }

    @Test
    public void credencialesValidasGeneranTokenYSession() {
        ClientAuthRequest request = new ClientAuthRequest();
        request.setClientId(CLIENT_ID);
        request.setClientSecret("hcen2025_periferico_secret_key");

        Response response = clientAuthResource.getToken(request);

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            fail("Respuesta inesperada (" + response.getStatus() + "): " + response.getEntity());
        }
        ClientAuthResponse body = (ClientAuthResponse) response.getEntity();
        assertNotNull("El token generado no debe ser nulo", body.getAccessToken());
        assertTrue("Debe existir al menos una sesión activa almacenada",
                jwtSessionDAO.findActiveByClientId(CLIENT_ID).size() >= 1);
    }

    @Test
    public void credencialesInvalidasDevuelvenUnauthorized() {
        ClientAuthRequest request = new ClientAuthRequest();
        request.setClientId(CLIENT_ID);
        request.setClientSecret("clave_incorrecta");

        Response response = clientAuthResource.getToken(request);

        if (response.getStatus() != Response.Status.UNAUTHORIZED.getStatusCode()) {
            fail("Respuesta inesperada (" + response.getStatus() + "): " + response.getEntity());
        }
        assertTrue("No deben crearse sesiones para credenciales inválidas",
                jwtSessionDAO.findActiveByClientId(CLIENT_ID).isEmpty());
    }
}
