package com.hcen.periferico.rest;

import com.hcen.periferico.dao.AdministradorClinicaDAO;
import com.hcen.periferico.dao.ClinicaDAO;
import com.hcen.periferico.entity.administrador_clinica;
import com.hcen.periferico.entity.clinica;
import com.hcen.periferico.service.AuthenticationService;
import com.hcen.periferico.service.ClinicaService;
import hcen.central.inus.testsupport.ArquillianMavenResolver;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Smoke IT del flujo de creación de clínica en el componente periférico.
 */
@RunWith(Arquillian.class)
public class ClinicaResourceIT {

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = ArquillianMavenResolver.resolve(
            "com.h2database:h2",
            "org.mindrot:jbcrypt",
            "org.hibernate.orm:hibernate-core:6.4.4.Final"
        );

        return ShrinkWrap.create(WebArchive.class, "clinica-resource-it.war")
            .addClasses(
                ClinicaResource.class,
                ClinicaService.class,
                ClinicaDAO.class,
                AuthenticationService.class,
                AdministradorClinicaDAO.class,
                clinica.class,
                administrador_clinica.class,
                com.hcen.periferico.entity.Especialidad.class,
                com.hcen.periferico.entity.profesional_salud.class,
                com.hcen.periferico.entity.UsuarioSalud.class,
                com.hcen.periferico.entity.documento_clinico.class,
                com.hcen.periferico.dao.ProfesionalSaludDAO.class,
                com.hcen.periferico.entity.configuracion_clinica.class,
                com.hcen.periferico.dao.ConfiguracionClinicaDAO.class
            )
            .addAsLibraries(libs)
            .addAsResource("META-INF/persistence-periferico-it.xml", "META-INF/persistence.xml")
            .addAsWebInfResource("test-ds/resources-periferico.xml", "resources.xml")
            .addAsWebInfResource("arquillian.xml", "arquillian.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private ClinicaResource clinicaResource;

    @EJB
    private ClinicaDAO clinicaDAO;

    @EJB
    private AdministradorClinicaDAO adminDAO;

    @Before
    public void cleanData() {
        adminDAO.findAll().forEach(adminDAO::delete);
        clinicaDAO.findAll().forEach(clinicaDAO::delete);
    }

    @Test
    public void crearClinicaGeneraAdminPorDefecto() {
        ClinicaResource.ClinicaCreateRequest request = new ClinicaResource.ClinicaCreateRequest();
        request.setNombre("Clinica IT");
        request.setDireccion("Av IT 123");
        request.setEmail("contacto@it.com");

        Response response = clinicaResource.createClinica(request);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        ClinicaResource.ClinicaDTO dto = (ClinicaResource.ClinicaDTO) response.getEntity();
        assertNotNull(dto.getTenantId());

        List<administrador_clinica> admins = adminDAO.findByTenant(java.util.UUID.fromString(dto.getTenantId()));
        assertEquals(1, admins.size());
        administrador_clinica admin = admins.get(0);
        assertTrue("Password debe guardarse hasheada", admin.getPassword() != null && admin.getPassword().length() > 10);
        assertFalse("Password no debe quedar en texto plano", "1234".equals(admin.getPassword()));

        assertEquals("Clinica IT", clinicaDAO.findByTenantId(java.util.UUID.fromString(dto.getTenantId()))
            .orElseThrow()
            .getNombre());
    }
}
