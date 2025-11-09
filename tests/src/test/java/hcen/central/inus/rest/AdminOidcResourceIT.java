package hcen.central.inus.rest;

import hcen.central.inus.dao.admin_hcen_dao;
import hcen.central.inus.dto.AdminLoginRequest;
import hcen.central.inus.entity.admin_hcen;
import hcen.central.inus.security.oidc.OIDCAuthenticationService;
import hcen.central.inus.security.oidc.OIDCCallbackHandler;
import hcen.central.inus.security.oidc.OIDCUserInfoService;
import hcen.central.inus.service.authentication_service;
import hcen.central.inus.testsupport.ArquillianMavenResolver;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integración mínima para {@link AdminAuthResource} y {@link OIDCAuthResource}.
 */
@RunWith(Arquillian.class)
public class AdminOidcResourceIT {

    private static final String ADMIN_USERNAME = "itest-admin";
    private static final String ADMIN_PASSWORD = "Admin2025!";

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve("com.h2database:h2");

        return ShrinkWrap.create(WebArchive.class, "admin-oidc-it.war")
                .addClasses(
                        AdminAuthResource.class,
                        authentication_service.class,
                        admin_hcen_dao.class,
                        admin_hcen.class,
                        AdminLoginRequest.class,
                        OIDCAuthResource.class,
                        OIDCAuthenticationService.class,
                        OIDCCallbackHandler.class,
                        OIDCUserInfoService.class
                )
                .addAsLibraries(libs)
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource("test-ds/resources.xml", "resources.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private AdminAuthResource adminAuthResource;

    @Inject
    private OIDCAuthResource oidcAuthResource;

    @EJB
    private authentication_service authenticationService;

    @EJB
    private admin_hcen_dao adminDao;

    @Before
    public void ensureCleanAdmin() {
        adminDao.findByUsernameIncludingInactive(ADMIN_USERNAME).ifPresent(admin -> adminDao.delete(admin.getId()));
        injectRequest(oidcAuthResource, createDummyRequest());
    }

    @Test
    public void loginAdminValidoDevuelveDatos() {
        adminDao.findByUsernameIncludingInactive(ADMIN_USERNAME).ifPresent(admin -> adminDao.delete(admin.getId()));
        admin_hcen created = authenticationService.createAdmin(ADMIN_USERNAME, ADMIN_PASSWORD, "Integracion", "Test", "itest@localhost");

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword(ADMIN_PASSWORD);

        Response response = adminAuthResource.login(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonObject body = (JsonObject) response.getEntity();
        assertNotNull(body);
        assertEquals(created.getUsername(), body.getString("username"));
        assertEquals(created.getEmail(), body.getString("email"));
        assertEquals(created.getFirstName(), body.getString("firstName"));
        assertEquals(created.getLastName(), body.getString("lastName"));
        assertEquals(Boolean.TRUE, body.getBoolean("active"));
    }

    @Test
    public void loginConCredencialesIncorrectasDevuelveUnauthorized() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("no-existe");
        request.setPassword("sin-clave");

        Response response = adminAuthResource.login(request);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void loginSinCredencialesDevuelveBadRequest() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("");
        request.setPassword("   ");

        Response response = adminAuthResource.login(request);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void loginOidcSinRedirectUriDevuelveBadRequest() {
        Response response = oidcAuthResource.login(null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    private static void injectRequest(OIDCAuthResource resource, HttpServletRequest request) {
        try {
            Field requestField = OIDCAuthResource.class.getDeclaredField("httpRequest");
            requestField.setAccessible(true);
            requestField.set(resource, request);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("No se pudo inyectar HttpServletRequest", e);
        }
    }

    private static HttpServletRequest createDummyRequest() {
        HttpSession session = createSession();
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class[]{HttpServletRequest.class},
                new HttpServletRequestHandler(session));
    }

    private static HttpSession createSession() {
        Map<String, Object> attributes = new HashMap<>();
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class[]{HttpSession.class},
                new HttpSessionHandler(attributes));
    }

    private static Object defaultPrimitiveValue(Class<?> type) {
        if (boolean.class.equals(type)) {
            return false;
        } else if (byte.class.equals(type)) {
            return (byte) 0;
        } else if (short.class.equals(type)) {
            return (short) 0;
        } else if (int.class.equals(type)) {
            return 0;
        } else if (long.class.equals(type)) {
            return 0L;
        } else if (float.class.equals(type)) {
            return 0F;
        } else if (double.class.equals(type)) {
            return 0D;
        } else if (char.class.equals(type)) {
            return '\0';
        }
        return null;
    }

    private static final class HttpServletRequestHandler implements InvocationHandler {
        private final HttpSession session;

        private HttpServletRequestHandler(HttpSession session) {
            this.session = session;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getHeader".equals(method.getName())) {
                return null;
            }
            if ("getServerName".equals(method.getName())) {
                return "localhost";
            }
            if ("getSession".equals(method.getName())) {
                if (args == null || args.length == 0) {
                    return session;
                }
                boolean create = (boolean) args[0];
                return create ? session : session;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType.isPrimitive()) {
                return defaultPrimitiveValue(returnType);
            }
            return null;
        }
    }

    private static final class HttpSessionHandler implements InvocationHandler {
        private final Map<String, Object> attributes;

        private HttpSessionHandler(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "getAttribute":
                    return attributes.get(args[0]);
                case "setAttribute":
                    attributes.put((String) args[0], args[1]);
                    return null;
                case "removeAttribute":
                    attributes.remove(args[0]);
                    return null;
                case "getId":
                    return "session-id";
                case "getCreationTime":
                case "getLastAccessedTime":
                    return 0L;
                case "getMaxInactiveInterval":
                    return 0;
                case "isNew":
                    return true;
                default:
                    Class<?> returnType = method.getReturnType();
                    if (returnType.isPrimitive()) {
                        return defaultPrimitiveValue(returnType);
                    }
                    return null;
            }
        }
    }
}
