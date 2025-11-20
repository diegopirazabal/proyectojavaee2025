package hcen.central.inus.rest;

import hcen.central.inus.dao.admin_hcen_dao;
import hcen.central.inus.dto.AdminLoginRequest;
import hcen.central.inus.entity.admin_hcen;
import hcen.central.inus.service.authentication_service;
import hcen.central.inus.testsupport.ArquillianMavenResolver;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.File;
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

/**
 * Verifica la autenticación de administradores contra la base en memoria.
 */
@RunWith(Arquillian.class)
public class AdminAuthResourceIT {

    private static final String USERNAME = "itest-admin";
    private static final String PASSWORD = "Admin2025!";

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve(
            "com.h2database:h2",
            "org.mindrot:jbcrypt"
        );

        return ShrinkWrap.create(WebArchive.class, "admin-auth-resource-it.war")
            .addClasses(
                AdminAuthResource.class,
                authentication_service.class,
                admin_hcen_dao.class,
                admin_hcen.class,
                AdminLoginRequest.class
            )
            .addAsLibraries(libs)
            .addAsResource("META-INF/persistence-auth-it.xml", "META-INF/persistence.xml")
            .addAsResource("sql/schema.sql", "sql/schema.sql")
            .addAsWebInfResource("test-ds/resources.xml", "resources.xml")
            .addAsWebInfResource("arquillian.xml", "arquillian.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private AdminAuthResource adminAuthResource;

    @EJB
    private authentication_service authenticationService;

    @EJB
    private admin_hcen_dao adminDao;

    @Before
    public void cleanAdmin() {
        adminDao.findByUsernameIncludingInactive(USERNAME)
            .ifPresent(admin -> adminDao.delete(admin.getId()));
    }

    @Test
    public void loginValidoDevuelveDatosDelAdministrador() {
        admin_hcen created = authenticationService.createAdmin(
            USERNAME, PASSWORD, "Integracion", "Test", "itest@localhost");

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);

        Response response = adminAuthResource.login(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        var body = (jakarta.json.JsonObject) response.getEntity();
        assertEquals(created.getUsername(), body.getString("username"));
        assertEquals(created.getEmail(), body.getString("email"));
    }

    @Test
    public void loginConCredencialesInvalidasDevuelveUnauthorized() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("unknown");
        request.setPassword("invalid");

        Response response = adminAuthResource.login(request);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
}
