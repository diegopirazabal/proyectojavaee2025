package hcen.central.inus.rest;

import hcen.central.inus.dao.UsuarioClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.ActualizarUsuarioSaludRequest;
import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.entity.UsuarioClinica;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.service.UsuarioSaludService;
import hcen.central.inus.testsupport.ArquillianMavenResolver;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import hcen.central.inus.testsupport.data.TestRegistrarUsuarioRequestFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas de integración para {@link UsuarioSaludResource} validando la superficie JAX-RS
 * sobre el servicio y el acceso a la base de datos embebida.
 */
@RunWith(Arquillian.class)
public class UsuarioSaludResourceIT {

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve("com.h2database:h2");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "usuario-salud-resource-it.war")
            .addClasses(
                UsuarioSaludResource.class,
                UsuarioSaludService.class,
                UsuarioSaludDAO.class,
                UsuarioClinicaDAO.class,
                RegistrarUsuarioRequest.class,
                UsuarioSaludDTO.class,
                UsuarioSalud.class,
                UsuarioClinica.class,
                hcen.central.inus.entity.converter.InstantAttributeConverter.class,
                hcen.central.inus.entity.converter.TipoDocumentoAttributeConverter.class,
                hcen.central.inus.util.TipoDocumentoMapper.class,
                TipoDocumento.class,
                ActualizarUsuarioSaludRequest.class
            )
            .addAsLibraries(libs)
            .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
            .addAsResource("sql/schema.sql", "sql/schema.sql")
            .addAsWebInfResource("test-ds/resources.xml", "resources.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        if (Boolean.getBoolean("shrinkwrap.export")) {
            war.as(ZipExporter.class).exportTo(new File("tests/target/usuario-salud-resource-it.war"), true);
        }
        return war;
    }

    @Inject
    private UsuarioSaludResource resource;

    @EJB
    private UsuarioSaludService usuarioSaludService;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

    @Test
    public void listaUsuariosPorTenant() {
        UUID tenantId = UUID.randomUUID();
        usuarioSaludService.registrarUsuarioEnClinica(
            TestRegistrarUsuarioRequestFactory.build("55566677", tenantId, "Lucia", "Suarez"));
        usuarioSaludService.registrarUsuarioEnClinica(
            TestRegistrarUsuarioRequestFactory.build("55566678", tenantId, "Luis", "Suarez"));

        Response listarTodos = resource.getUsuariosByTenantId(tenantId.toString(), null);
        assertEquals(Response.Status.OK.getStatusCode(), listarTodos.getStatus());
        @SuppressWarnings("unchecked")
        List<UsuarioSaludDTO> todos = (List<UsuarioSaludDTO>) listarTodos.getEntity();
        assertEquals(2, todos.size());
    }

    @Test
    public void validarParametrosDeEntrada() {
        Response sinTenant = resource.getUsuariosByTenantId("", null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), sinTenant.getStatus());

        Response tenantInvalido = resource.desasociarUsuarioDeClinica("12345678", "tenant-no-uuid");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), tenantInvalido.getStatus());
    }

    @Test
    public void verificarUsuarioExisteRetornaResultado() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "88119933";
        usuarioSaludService.registrarUsuarioEnClinica(
            TestRegistrarUsuarioRequestFactory.build(cedula, tenantId, "Mauro", "Gomez"));

        Response response = resource.verificarUsuarioExiste(cedula);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertEquals(Boolean.TRUE, body.get("existe"));
        assertEquals(cedula, body.get("cedula"));
    }

    @Test
    public void registrarYConsultarUsuario() {
        UUID tenantId = UUID.randomUUID();
        Response respuesta = resource.registrarUsuario(
            TestRegistrarUsuarioRequestFactory.build("88112233", tenantId, "Maria", "Lopez"));
        assertEquals(Response.Status.OK.getStatusCode(), respuesta.getStatus());
        UsuarioSaludDTO dto = (UsuarioSaludDTO) respuesta.getEntity();
        assertEquals("88112233", dto.getCedula());
        assertTrue(usuarioSaludDAO.existsByCedulaAndTenantId("88112233", tenantId));

        Response detalle = resource.getUsuarioByCedula("88112233");
        assertEquals(Response.Status.OK.getStatusCode(), detalle.getStatus());

        Response inexistente = resource.getUsuarioByCedula("00000000");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), inexistente.getStatus());
    }

    @Test
    public void desasociarUsuarioPropagaEstadoHttp() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "77118855";
        usuarioSaludService.registrarUsuarioEnClinica(
            TestRegistrarUsuarioRequestFactory.build(cedula, tenantId, "Pablo", "Ramos"));

        Response ok = resource.desasociarUsuarioDeClinica(cedula, tenantId.toString());
        assertEquals(Response.Status.OK.getStatusCode(), ok.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) ok.getEntity();
        assertTrue(body.get("message").contains("exitosamente"));

        Response notFound = resource.desasociarUsuarioDeClinica(cedula, tenantId.toString());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), notFound.getStatus());
    }

}
