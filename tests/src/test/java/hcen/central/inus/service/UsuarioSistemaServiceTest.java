package hcen.central.inus.service;

import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.UsuarioSistemaResponse;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.enums.UsuarioSistemaTipo;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UsuarioSistemaServiceTest {

    private UsuarioSistemaService service;
    private StubUsuarioSaludDAO usuarioSaludDAO;
    private StubPerifericoUsuariosClient perifericoClient;

    @Before
    public void setUp() throws Exception {
        usuarioSaludDAO = new StubUsuarioSaludDAO();
        perifericoClient = new StubPerifericoUsuariosClient();
        service = new UsuarioSistemaService();
        inject(service, "usuarioSaludDAO", usuarioSaludDAO);
        inject(service, "perifericoUsuariosClient", perifericoClient);
    }

    @Test
    public void combinaResultadosDeFuentes() {
        UsuarioSalud local = buildUsuarioSalud();
        usuarioSaludDAO.setResponse(List.of(local));

        UsuarioSistemaResponse profesional = new UsuarioSistemaResponse();
        profesional.setTipo(UsuarioSistemaTipo.PROFESIONAL_SALUD);
        profesional.setOrigen("PERIFERICO");
        profesional.setNumeroDocumento("9876543");
        profesional.setNombreCompleto("Dr. Gregory House");
        perifericoClient.setProfesionales(List.of(profesional));

        UsuarioSistemaResponse admin = new UsuarioSistemaResponse();
        admin.setTipo(UsuarioSistemaTipo.ADMINISTRADOR_CLINICA);
        admin.setOrigen("PERIFERICO");
        admin.setNombreCompleto("Carla Admin");
        perifericoClient.setAdministradores(List.of(admin));

        List<UsuarioSistemaResponse> resultado = service.obtenerCatalogo(null, null, "Lucia", "Suarez", null);

        assertEquals(3, resultado.size());

        UsuarioSistemaResponse primero = resultado.get(0);
        assertEquals(UsuarioSistemaTipo.USUARIO_SALUD, primero.getTipo());
        assertEquals("CENTRAL", primero.getOrigen());
        assertEquals("12345678", primero.getNumeroDocumento());
        assertEquals("Lucia Maria Suarez", primero.getNombreCompleto());
        assertEquals("lucia@example.com", primero.getEmail());
        assertEquals(Boolean.TRUE, primero.getActivo());
        assertEquals("1990-05-01", primero.getFechaNacimiento());

        assertEquals("Lucia", usuarioSaludDAO.lastNombre);
        assertEquals("Suarez", usuarioSaludDAO.lastApellido);
        assertEquals(0, usuarioSaludDAO.lastPage);
        assertEquals(150, usuarioSaludDAO.lastSize); // límite por defecto

        assertEquals(1, perifericoClient.profesionalesCalls);
        assertEquals(1, perifericoClient.administradoresCalls);
        assertEquals(150, perifericoClient.lastProfesionalesLimit);
        assertEquals(150, perifericoClient.lastAdministradoresLimit);
        assertEquals("Lucia", perifericoClient.lastProfesionalesNombre);
        assertEquals("Suarez", perifericoClient.lastProfesionalesApellido);
        assertEquals("Lucia", perifericoClient.lastAdministradoresNombre);
        assertEquals("Suarez", perifericoClient.lastAdministradoresApellido);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaTipoDocumentoDesconocido() {
        service.obtenerCatalogo("dni-extranjero", "123", null, null, null);
    }

    @Test
    public void omiteConsultasRemotasCuandoTipoDocumentoNoPermiteBusquedaPorCI() {
        UsuarioSalud local = buildUsuarioSalud();
        usuarioSaludDAO.setResponse(List.of(local));

        List<UsuarioSistemaResponse> resultado = service.obtenerCatalogo("PA", "1234", null, null, 25);

        assertEquals(1, resultado.size());

        assertEquals(1, usuarioSaludDAO.callCount);
        assertEquals(TipoDocumento.PA, usuarioSaludDAO.lastTipoDocumento);
        assertEquals("1234", usuarioSaludDAO.lastNumeroDocumento);

        assertEquals(0, perifericoClient.profesionalesCalls);
        assertEquals(0, perifericoClient.administradoresCalls);
    }

    @Test
    public void respetaLimiteMaximoYPropagaValorNormalizado() {
        UsuarioSalud local = buildUsuarioSalud();
        usuarioSaludDAO.setResponse(List.of(local));

        UsuarioSistemaResponse profesional = new UsuarioSistemaResponse();
        profesional.setTipo(UsuarioSistemaTipo.PROFESIONAL_SALUD);
        perifericoClient.setProfesionales(List.of(profesional));

        List<UsuarioSistemaResponse> resultado = service.obtenerCatalogo("DO", "4567", null, null, 900);

        assertTrue(resultado.size() >= 1);

        assertEquals(500, usuarioSaludDAO.lastSize);
        assertEquals(500, perifericoClient.lastProfesionalesLimit);
        assertSame(TipoDocumento.DO, perifericoClient.lastProfesionalesTipoDocumento);
        assertEquals("4567", perifericoClient.lastProfesionalesNumeroDocumento);
    }

    private static UsuarioSalud buildUsuarioSalud() {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setId(42L);
        usuario.setCedula("12345678");
        usuario.setTipoDeDocumento(TipoDocumento.DO);
        usuario.setPrimerNombre("Lucia");
        usuario.setSegundoNombre("Maria");
        usuario.setPrimerApellido("Suarez");
        usuario.setEmail("lucia@example.com");
        usuario.setActive(true);
        usuario.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        return usuario;
    }

    private static class StubUsuarioSaludDAO extends UsuarioSaludDAO {
        private List<UsuarioSalud> response = new ArrayList<>();
        private TipoDocumento lastTipoDocumento;
        private String lastNumeroDocumento;
        private String lastNombre;
        private String lastApellido;
        private int lastPage;
        private int lastSize;
        private int callCount;

        void setResponse(List<UsuarioSalud> response) {
            this.response = new ArrayList<>(response);
        }

        @Override
        public List<UsuarioSalud> findByFilters(TipoDocumento tipoDocumento,
                                                String numeroDocumento,
                                                String nombre,
                                                String apellido,
                                                int page,
                                                int size) {
            this.lastTipoDocumento = tipoDocumento;
            this.lastNumeroDocumento = numeroDocumento;
            this.lastNombre = nombre;
            this.lastApellido = apellido;
            this.lastPage = page;
            this.lastSize = size;
            this.callCount++;
            return new ArrayList<>(response);
        }
    }

    private static class StubPerifericoUsuariosClient extends PerifericoUsuariosClient {
        private List<UsuarioSistemaResponse> profesionales = new ArrayList<>();
        private List<UsuarioSistemaResponse> administradores = new ArrayList<>();
        private int profesionalesCalls;
        private int administradoresCalls;
        private int lastProfesionalesLimit;
        private int lastAdministradoresLimit;
        private TipoDocumento lastProfesionalesTipoDocumento;
        private String lastProfesionalesNumeroDocumento;
        private String lastProfesionalesNombre;
        private String lastProfesionalesApellido;
        private String lastAdministradoresNombre;
        private String lastAdministradoresApellido;

        void setProfesionales(List<UsuarioSistemaResponse> profesionales) {
            this.profesionales = new ArrayList<>(profesionales);
        }

        void setAdministradores(List<UsuarioSistemaResponse> administradores) {
            this.administradores = new ArrayList<>(administradores);
        }

        @Override
        public List<UsuarioSistemaResponse> listarProfesionales(String numeroDocumento,
                                                                 TipoDocumento tipoDocumento,
                                                                 String nombre,
                                                                 String apellido,
                                                                 int limit) {
            this.profesionalesCalls++;
            this.lastProfesionalesNumeroDocumento = numeroDocumento;
            this.lastProfesionalesTipoDocumento = tipoDocumento;
            this.lastProfesionalesNombre = nombre;
            this.lastProfesionalesApellido = apellido;
            this.lastProfesionalesLimit = limit;
            return new ArrayList<>(profesionales);
        }

        @Override
        public List<UsuarioSistemaResponse> listarAdministradores(String nombre,
                                                                  String apellido,
                                                                  int limit) {
            this.administradoresCalls++;
            this.lastAdministradoresNombre = nombre;
            this.lastAdministradoresApellido = apellido;
            this.lastAdministradoresLimit = limit;
            return new ArrayList<>(administradores);
        }
    }

    private static void inject(UsuarioSistemaService target, String field, Object value) throws Exception {
        Field declaredField = UsuarioSistemaService.class.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);
    }
}
