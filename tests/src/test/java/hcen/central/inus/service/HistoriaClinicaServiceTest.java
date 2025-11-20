package hcen.central.inus.service;

import hcen.central.inus.dao.HistoriaClinicaDAO;
import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.DocumentoClinicoDTO;
import hcen.central.inus.dto.HistoriaClinicaDocumentoDetalleResponse;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.historia_clinica_documento;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class HistoriaClinicaServiceTest {

    private HistoriaClinicaService service;
    private HistoriaClinicaDAO historiaDAO;
    private UsuarioSaludDAO usuarioDAO;
    private PerifericoDocumentosClient perifericoClient;

    @Before
    public void setUp() throws Exception {
        service = new HistoriaClinicaService();
        historiaDAO = mock(HistoriaClinicaDAO.class);
        usuarioDAO = mock(UsuarioSaludDAO.class);
        perifericoClient = mock(PerifericoDocumentosClient.class);
        inject("historiaDAO", historiaDAO);
        inject("usuarioSaludDAO", usuarioDAO);
        inject("perifericoDocumentosClient", perifericoClient);
    }

    @Test
    public void registrarDocumentoCreaHistoriaYDocumento() throws Exception {
        UsuarioSalud usuario = usuario("12345678");
        when(usuarioDAO.findByCedula("12345678")).thenReturn(Optional.of(usuario));
        when(historiaDAO.findByUsuario(usuario)).thenReturn(Optional.empty());

        historia_clinica persisted = historia(usuario, UUID.randomUUID());
        when(historiaDAO.save(any(historia_clinica.class))).thenReturn(persisted);
        when(historiaDAO.existsDocumento(eq(persisted.getId()), any())).thenReturn(false);

        UUID docId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        historia_clinica_documento savedDoc = new historia_clinica_documento();
        when(historiaDAO.saveDocumento(any(historia_clinica_documento.class))).thenReturn(savedDoc);

        UUID resultado = service.registrarDocumento("12345678", tenantId, docId);

        assertEquals(persisted.getId(), resultado);
        verify(historiaDAO).saveDocumento(argThat(doc -> doc.getDocumentoId().equals(docId) &&
            doc.getTenantId().equals(tenantId)));
        verify(historiaDAO).save(persisted);
    }

    @Test
    public void registrarDocumentoNoDuplicaDocumentosExistentes() throws Exception {
        UsuarioSalud usuario = usuario("22223333");
        when(usuarioDAO.findByCedula("22223333")).thenReturn(Optional.of(usuario));
        historia_clinica historia = historia(usuario, UUID.randomUUID());
        when(historiaDAO.findByUsuario(usuario)).thenReturn(Optional.of(historia));
        when(historiaDAO.existsDocumento(eq(historia.getId()), any())).thenReturn(true);

        UUID doc = UUID.randomUUID();
        UUID historiaId = service.registrarDocumento("22223333", UUID.randomUUID(), doc);

        assertEquals(historia.getId(), historiaId);
        verify(historiaDAO, never()).saveDocumento(any());
    }

    @Test
    public void obtenerDocumentosIncluyeDatosDelPeriferico() throws Exception {
        UsuarioSalud usuario = usuario("55566677");
        when(usuarioDAO.findByCedula("55566677")).thenReturn(Optional.of(usuario));

        historia_clinica historia = historia(usuario, UUID.randomUUID());
        when(historiaDAO.findByUsuario(usuario)).thenReturn(Optional.of(historia));

        historia_clinica_documento documento = new historia_clinica_documento();
        setField(documento, "historiaClinica", historia);
        documento.setDocumentoId(UUID.randomUUID());
        documento.setTenantId(UUID.randomUUID());
        documento.setFecRegistro(LocalDateTime.now());
        when(historiaDAO.findDocumentosByHistoria(historia.getId())).thenReturn(List.of(documento));

        DocumentoClinicoDTO periferico = new DocumentoClinicoDTO();
        periferico.setId("ABC");
        periferico.setCodigoMotivoConsulta("COD");
        periferico.setNombreCompletoProfesional("Dr. Test");
        periferico.setNombreClinica("Clinica Uno");
        when(perifericoClient.obtenerDocumento(documento.getDocumentoId(), documento.getTenantId()))
            .thenReturn(Optional.of(periferico));

        List<HistoriaClinicaDocumentoDetalleResponse> respuesta = service.obtenerDocumentosPorCedula("55566677");

        assertEquals(1, respuesta.size());
        HistoriaClinicaDocumentoDetalleResponse dto = respuesta.get(0);
        assertEquals(historia.getId().toString(), dto.getHistoriaId());
        assertEquals("Clinica Uno", dto.getNombreClinica());
        assertEquals("Dr. Test", dto.getProfesional());
        verify(perifericoClient).obtenerDocumento(documento.getDocumentoId(), documento.getTenantId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void obtenerDocumentosRequiereUsuarioExistente() {
        when(usuarioDAO.findByCedula("999")).thenReturn(Optional.empty());
        service.obtenerDocumentosPorCedula("999");
    }

    private UsuarioSalud usuario(String cedula) {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCedula(cedula);
        return usuario;
    }

    private historia_clinica historia(UsuarioSalud usuario, UUID id) throws Exception {
        historia_clinica historia = new historia_clinica();
        historia.setUsuario(usuario);
        historia.setUsuarioCedula(usuario.getCedula());
        setField(historia, "id", id);
        historia.setFecCreacion(LocalDateTime.now());
        historia.setFecActualizacion(LocalDateTime.now());
        return historia;
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = HistoriaClinicaService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static void setField(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
