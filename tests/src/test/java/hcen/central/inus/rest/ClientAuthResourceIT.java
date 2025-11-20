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
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Smoke test de integración para {@link ClientAuthResource} verificando el flujo completo
 * con persistencia real sobre H2 embebido.
 */
@RunWith(Arquillian.class)
public class ClientAuthResourceIT {

    private static final String CLIENT_ID = "componente-periferico";
    private static final String CLIENT_SECRET = "hcen2025_periferico_secret_key";

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve(
            "com.h2database:h2",
            "io.jsonwebtoken:jjwt-api",
            "io.jsonwebtoken:jjwt-impl",
            "io.jsonwebtoken:jjwt-jackson"
        );

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
                InvalidTokenException.class
            )
            .addAsLibraries(libs)
            .addAsResource("META-INF/persistence-auth-it.xml", "META-INF/persistence.xml")
            .addAsResource("META-INF/oidc-config.properties", "META-INF/oidc-config.properties")
            .addAsResource("sql/schema.sql", "sql/schema.sql")
            .addAsWebInfResource("test-ds/resources.xml", "resources.xml")
            .addAsWebInfResource("arquillian.xml", "arquillian.xml")
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
    public void credencialesValidasGeneranTokenYPersistenSesion() {
        ClientAuthRequest request = new ClientAuthRequest();
        request.setClientId(CLIENT_ID);
        request.setClientSecret(CLIENT_SECRET);

        Response response = clientAuthResource.getToken(request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        ClientAuthResponse body = (ClientAuthResponse) response.getEntity();
        assertNotNull(body.getAccessToken());
        assertTrue(jwtSessionDAO.findActiveByClientId(CLIENT_ID).size() >= 1);
    }

    @Test
    public void credencialesInvalidasDevuelvenUnauthorized() {
        ClientAuthRequest request = new ClientAuthRequest();
        request.setClientId(CLIENT_ID);
        request.setClientSecret("clave-incorrecta");

        Response response = clientAuthResource.getToken(request);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
}
