package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioClinica;
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

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Pruebas de integración para {@link UsuarioClinicaDAO} sobre TomEE embebido.
 * Valida la persistencia y consultas clave usando H2 en memoria.
 */
@RunWith(Arquillian.class)
public class UsuarioClinicaDAOIT {

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve("com.h2database:h2");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "usuario-clinica-dao-it.war")
            .addClasses(
                UsuarioClinicaDAO.class,
                UsuarioClinica.class
            )
            .addAsLibraries(libs)
            .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
            .addAsResource("sql/schema.sql", "sql/schema.sql")
            .addAsWebInfResource("test-ds/resources.xml", "resources.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        if (Boolean.getBoolean("shrinkwrap.export")) {
            war.as(ZipExporter.class).exportTo(new File("tests/target/usuario-clinica-dao-it.war"), true);
        }
        return war;
    }

    @EJB
    private UsuarioClinicaDAO usuarioClinicaDAO;

    @Test
    public void guardaYRecuperaAsociacion() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "19998888";

        UsuarioClinica asociacion = buildAssociation(cedula, tenantId);
        usuarioClinicaDAO.save(asociacion);

        assertNotNull("el ID se genera al persistir", asociacion.getId());
        Optional<UsuarioClinica> buscada = usuarioClinicaDAO.findByUsuarioCedulaAndTenantId(cedula, tenantId);
        assertTrue(buscada.isPresent());
        assertTrue(buscada.get().isActive());
        assertTrue(usuarioClinicaDAO.existsAssociation(cedula, tenantId));
    }

    @Test
    public void paginacionYConteoPorTenant() {
        UUID tenantId = UUID.randomUUID();
        usuarioClinicaDAO.save(buildAssociation("11111111", tenantId));
        usuarioClinicaDAO.save(buildAssociation("22222222", tenantId));
        usuarioClinicaDAO.save(buildAssociation("33333333", tenantId));

        List<UsuarioClinica> pagina0 = usuarioClinicaDAO.findByTenantIdPaginated(tenantId, 0, 2);
        List<UsuarioClinica> pagina1 = usuarioClinicaDAO.findByTenantIdPaginated(tenantId, 1, 2);

        assertEquals(2, pagina0.size());
        assertEquals(1, pagina1.size());
        assertEquals(3, usuarioClinicaDAO.countByTenantId(tenantId));
    }

    @Test
    public void desactivaYEliminaAsociacion() {
        UUID tenantId = UUID.randomUUID();
        String cedula = "55554444";
        usuarioClinicaDAO.save(buildAssociation(cedula, tenantId));

        usuarioClinicaDAO.deactivateAssociation(cedula, tenantId);
        assertFalse("la asociación queda inactiva", usuarioClinicaDAO.existsAssociation(cedula, tenantId));

        Optional<UsuarioClinica> inactiva = usuarioClinicaDAO.findByUsuarioCedulaAndTenantId(cedula, tenantId);
        assertTrue(inactiva.isPresent());
        assertFalse(inactiva.get().isActive());
        assertTrue(usuarioClinicaDAO.findByTenantId(tenantId).isEmpty());

        assertTrue(usuarioClinicaDAO.deleteByUsuarioCedulaAndTenantId(cedula, tenantId));
        assertTrue(usuarioClinicaDAO.findByUsuarioCedulaAndTenantId(cedula, tenantId).isEmpty());
    }

    private UsuarioClinica buildAssociation(String cedula, UUID tenantId) {
        UsuarioClinica asociacion = new UsuarioClinica();
        asociacion.setUsuarioCedula(cedula);
        asociacion.setTenantId(tenantId);
        return asociacion;
    }
}
