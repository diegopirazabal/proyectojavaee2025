package hcen.central.inus.service;

import hcen.central.inus.dao.HistoriaClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.DocumentoClinicoDTO;
import hcen.central.inus.dto.HistoriaClinicaDocumentoDetalleResponse;
import hcen.central.inus.dto.HistoriaClinicaIdResponse;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.historia_clinica_documento;
import hcen.central.inus.entity.converter.TipoDocumentoAttributeConverter;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.testsupport.ArquillianMavenResolver;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class HistoriaClinicaServiceIT {

    @Deployment
    public static WebArchive createDeployment() {
        var libs = ArquillianMavenResolver.resolve("com.h2database:h2");

        return ShrinkWrap.create(WebArchive.class, "historia-clinica-service-it.war")
            .addClasses(
                HistoriaClinicaService.class,
                HistoriaClinicaDAO.class,
                UsuarioSaludDAO.class,
                PerifericoDocumentosClient.class, // stub en tests
                DocumentoClinicoDTO.class,
                HistoriaClinicaDocumentoDetalleResponse.class,
                HistoriaClinicaIdResponse.class,
                UsuarioSalud.class,
                historia_clinica.class,
                historia_clinica_documento.class,
                hcen.central.inus.entity.converter.UUIDStringConverter.class,
                hcen.central.inus.entity.converter.InstantTimestampConverter.class,
                TipoDocumento.class,
                TipoDocumentoAttributeConverter.class
            )
            .addAsLibraries(libs)
            .addAsResource("META-INF/persistence-historia-it.xml", "META-INF/persistence.xml")
            .addAsWebInfResource("test-ds/resources.xml", "resources.xml")
            .addAsWebInfResource("arquillian.xml", "arquillian.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @EJB
    private HistoriaClinicaService historiaClinicaService;

    @EJB
    private UsuarioSaludDAO usuarioSaludDAO;

    @PersistenceContext(unitName = "hcen-central-pu")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Before
    public void cleanDatabase() throws Exception {
        try {
            tx.begin();
            em.joinTransaction();
            em.createQuery("DELETE FROM historia_clinica_documento").executeUpdate();
            em.createQuery("DELETE FROM historia_clinica").executeUpdate();
            em.createQuery("DELETE FROM UsuarioSalud").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Exception ignored) {
                // no-op
            }
            throw e;
        } finally {
            PerifericoDocumentosClient.reset();
        }
    }

    @Test
    public void obtieneDocumentosDesdePerifericoEnBatch() {
        UUID documentoId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        DocumentoClinicoDTO periferico = new DocumentoClinicoDTO();
        periferico.setId(documentoId.toString());
        periferico.setTenantId(tenantId.toString());
        periferico.setNombreClinica("Clinica Periferica IT");
        periferico.setNombreCompletoProfesional("Dr. Periferico");
        periferico.setCodigoMotivoConsulta("COD-123");
        periferico.setFechaInicioDiagnostico("2025-01-01T10:00:00");
        PerifericoDocumentosClient.setBatchResponse(List.of(periferico));

        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCedula("88889999");
        usuario.setEmail("it-user@hcen.local");
        usuario.setPrimerNombre("Integracion");
        usuario.setPrimerApellido("Test");
        usuario.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        usuarioSaludDAO.save(usuario);

        historiaClinicaService.registrarDocumento(usuario.getCedula(), tenantId, documentoId);

        List<HistoriaClinicaDocumentoDetalleResponse> resultado = historiaClinicaService
            .obtenerDocumentosPorCedula(usuario.getCedula());

        assertEquals(1, resultado.size());
        HistoriaClinicaDocumentoDetalleResponse dto = resultado.get(0);
        assertEquals(documentoId.toString(), dto.getDocumentoId());
        assertEquals(tenantId.toString(), dto.getTenantId());
        assertEquals("Clinica Periferica IT", dto.getNombreClinica());
        assertEquals("Dr. Periferico", dto.getProfesional());
        assertEquals("COD-123", dto.getMotivoConsulta());
        assertTrue("Debe usar batch /documentos", PerifericoDocumentosClient.getLastPath().contains("/documentos?ids="));
    }
}
