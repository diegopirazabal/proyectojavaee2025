package hcen.central.inus.service;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import hcen.central.inus.dao.UsuarioClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.ActualizarUsuarioSaludRequest;
import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.entity.UsuarioClinica;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.testsupport.ArquillianMavenResolver;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Prueba de integración de {@link UsuarioSaludService} sobre un contenedor embebido TomEE.
 * Se utiliza H2 como base de datos en memoria para validar la lógica de registro y validaciones.
 */
@RunWith(Arquillian.class)
public class UsuarioSaludServiceIT {

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve("com.h2database:h2");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "usuario-salud-service-it.war")
                .addClasses(
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
                        ActualizarUsuarioSaludRequest.class,
                        TipoDocumento.class
                )
                .addAsLibraries(libs)
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsResource("sql/schema.sql", "sql/schema.sql")
                .addAsWebInfResource("test-ds/resources.xml", "resources.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        if (Boolean.getBoolean("shrinkwrap.export")) {
            war.as(ZipExporter.class).exportTo(new File("tests/target/usuario-salud-service-it.war"), true);
        }

        return war;
    }

    @EJB
    private UsuarioSaludService usuarioSaludService;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

    @Test
    public void registraUsuarioNuevoYLoPersiste() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "12345670";

        RegistrarUsuarioRequest request = buildRequest(cedula, tenantId);
        UsuarioSaludDTO dto = usuarioSaludService.registrarUsuarioEnClinica(request);

        assertEquals(cedula, dto.getCedula());
        assertEquals("Ana Test", dto.getNombreCompleto());
        assertTrue(usuarioSaludDAO.existsByCedula(cedula));
        assertTrue(usuarioSaludDAO.existsByCedulaAndTenantId(cedula, tenantId));
    }

    @Test
    public void rechazaRegistroDuplicadoMismaClinica() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "45678901";

        usuarioSaludService.registrarUsuarioEnClinica(buildRequest(cedula, tenantId));

        try {
            usuarioSaludService.registrarUsuarioEnClinica(buildRequest(cedula, tenantId));
            throw new AssertionError("Se esperaba IllegalArgumentException envuelta en EJBException");
        } catch (jakarta.ejb.EJBException ex) {
            Throwable cause = ex.getCause();
            if (!(cause instanceof IllegalArgumentException)) {
                throw ex;
            }
        }
    }

    @Test
    public void obtieneUsuariosPorCedulaYTenant() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "99911122";

        usuarioSaludService.registrarUsuarioEnClinica(buildRequest(cedula, tenantId, "Lucia", "Suarez"));

        assertTrue(usuarioSaludService.verificarUsuarioExiste(cedula));
        assertTrue(usuarioSaludDAO.existsByCedulaAndTenantId(cedula, tenantId));

        UsuarioSaludDTO dto = usuarioSaludService.getUsuarioByCedula(cedula)
            .orElseThrow(() -> new AssertionError("No se obtuvo el usuario"));
        assertEquals("Lucia Suarez", dto.getNombreCompleto());

        List<UsuarioSaludDTO> porTenant = usuarioSaludService.getUsuariosByTenantId(tenantId);
        assertEquals(1, porTenant.size());
        assertEquals(cedula, porTenant.get(0).getCedula());
    }

    @Test
    public void listaUsuariosPorTenantOrdenados() {
        UUID tenantId = UUID.randomUUID();
        usuarioSaludService.registrarUsuarioEnClinica(buildRequest("10020030", tenantId, "Maria", "Rodriguez"));
        usuarioSaludService.registrarUsuarioEnClinica(buildRequest("10020031", tenantId, "Andrea", "Alonso"));

        List<UsuarioSaludDTO> usuarios = usuarioSaludService.getUsuariosByTenantId(tenantId);
        assertEquals(2, usuarios.size());
        assertEquals("Andrea", usuarios.get(0).getPrimerNombre());
        assertEquals("Alonso", usuarios.get(0).getPrimerApellido());
        assertEquals("Rodriguez", usuarios.get(1).getPrimerApellido());
    }

    @Test
    public void actualizaUsuarioYNormalizaDatos() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "77733388";
        usuarioSaludService.registrarUsuarioEnClinica(buildRequest(cedula, tenantId, "Pablo", "Pereyra"));

        ActualizarUsuarioSaludRequest update = new ActualizarUsuarioSaludRequest();
        update.setPrimerNombre("PABLO  ");
        update.setSegundoNombre("   Andres");
        update.setPrimerApellido("  Pereyra");
        update.setSegundoApellido("Lopez  ");
        update.setEmail("nuevo.correo@example.com");
        update.setActivo(Boolean.FALSE);
        update.setFechaNacimiento(LocalDate.of(1988, 2, 10).toString());

        UsuarioSaludDTO actualizado = usuarioSaludService.actualizarUsuario(cedula, update);

        assertEquals("PABLO Andres Pereyra Lopez", actualizado.getNombreCompleto());
        assertEquals("nuevo.correo@example.com", actualizado.getEmail());
        assertFalse(actualizado.getActive());
        assertEquals(LocalDate.of(1988, 2, 10), actualizado.getFechaNacimiento());
    }

    @Test
    public void desasociaUsuarioDeClinica() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "31415926";

        usuarioSaludService.registrarUsuarioEnClinica(buildRequest(cedula, tenantId));
        assertTrue(usuarioSaludService.desasociarUsuarioDeClinica(cedula, tenantId));
        assertFalse(usuarioSaludService.desasociarUsuarioDeClinica(cedula, tenantId));
        assertFalse(usuarioSaludDAO.existsByCedulaAndTenantId(cedula, tenantId));
    }

    private RegistrarUsuarioRequest buildRequest(String cedula, UUID tenantId) {
        return buildRequest(cedula, tenantId, "Ana", "Test");
    }

    private RegistrarUsuarioRequest buildRequest(String cedula, UUID tenantId, String primerNombre, String primerApellido) {
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest();
        request.setCedula(cedula);
        request.setTipoDocumento(TipoDocumento.DO);
        request.setPrimerNombre(primerNombre);
        request.setSegundoNombre(null);
        request.setPrimerApellido(primerApellido);
        request.setSegundoApellido(null);
        request.setEmail(primerNombre.toLowerCase() + ".test@example.com");
        request.setFechaNacimiento(LocalDate.of(1990, 1, 15));
        request.setTenantId(tenantId);
        return request;
    }
}
