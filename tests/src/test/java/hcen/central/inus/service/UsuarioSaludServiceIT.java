package hcen.central.inus.service;

import java.io.File;
import java.time.LocalDate;
import java.util.UUID;

import hcen.central.inus.testsupport.ArquillianMavenResolver;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.arquillian.container.test.api.Deployment;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.EJB;
import hcen.central.inus.dao.UsuarioClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.ActualizarUsuarioSaludRequest;
import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.entity.UsuarioClinica;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Prueba de integración de {@link UsuarioSaludService} sobre un contenedor embebido TomEE.
 * Se utiliza H2 como base de datos en memoria para validar la lógica de registro y validaciones.
 */
@RunWith(Arquillian.class)
public class UsuarioSaludServiceIT {

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve("com.h2database:h2");

        System.out.println(">>> UsuarioSalud class location: " + UsuarioSalud.class.getProtectionDomain().getCodeSource());

        WebArchive war = ShrinkWrap.create(WebArchive.class, "usuario-salud-service-it.war")
                .addClasses(
                        UsuarioSaludService.class,
                        UsuarioSaludDAO.class,
                        UsuarioClinicaDAO.class,
                        RegistrarUsuarioRequest.class,
                        UsuarioSaludDTO.class,
                        UsuarioSalud.class,
                        UsuarioClinica.class,
                        hcen.central.inus.testsupport.converter.TestInstantAttributeConverter.class,
                        hcen.central.inus.testsupport.converter.TestUUIDAttributeConverter.class,
                        ActualizarUsuarioSaludRequest.class,
                        TipoDocumento.class
                )
                .addAsLibraries(libs)
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
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

    private RegistrarUsuarioRequest buildRequest(String cedula, UUID tenantId) {
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest();
        request.setCedula(cedula);
        request.setTipoDocumento(TipoDocumento.DO);
        request.setPrimerNombre("Ana");
        request.setSegundoNombre(null);
        request.setPrimerApellido("Test");
        request.setSegundoApellido(null);
        request.setEmail("ana.test@example.com");
        request.setFechaNacimiento(LocalDate.of(1990, 1, 15));
        request.setTenantId(tenantId);
        return request;
    }
}
