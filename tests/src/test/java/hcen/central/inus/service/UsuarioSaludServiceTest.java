package hcen.central.inus.service;

import hcen.central.inus.dao.UsuarioSaludDAO;
import hcen.central.inus.dto.ActualizarUsuarioSaludRequest;
import hcen.central.inus.dto.DnicCiudadanoDTO;
import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.exception.UsuarioMenorDeEdadException;
import hcen.central.inus.testsupport.data.TestRegistrarUsuarioRequestFactory;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link UsuarioSaludService}.
 * Se ejercitan los flujos principales sin depender de una base real ni del servicio DNIC.
 */
public class UsuarioSaludServiceTest {

    private UsuarioSaludService service;
    private UsuarioSaludDAO usuarioDAO;
    private DnicServiceClient dnicClient;
    private EdadValidacionService edadValidacionService;

    @Before
    public void setUp() throws Exception {
        service = new UsuarioSaludService();
        usuarioDAO = mock(UsuarioSaludDAO.class);
        dnicClient = mock(DnicServiceClient.class);
        edadValidacionService = mock(EdadValidacionService.class);
        inject(service, "usuarioDAO", usuarioDAO);
        inject(service, "dnicClient", dnicClient);
        inject(service, "edadValidacionService", edadValidacionService);
    }

    @Test
    public void verificarUsuarioExisteDelegatesToDao() {
        when(usuarioDAO.existsByCedula("12345678")).thenReturn(true);
        assertTrue(service.verificarUsuarioExiste("12345678"));
        verify(usuarioDAO).existsByCedula("12345678");
    }

    @Test(expected = IllegalArgumentException.class)
    public void verificarUsuarioExisteValidaEntrada() {
        service.verificarUsuarioExiste("   ");
    }

    @Test
    public void registrarUsuarioNuevoMapeaDatosDeDnic() throws Exception {
        RegistrarUsuarioRequest request = TestRegistrarUsuarioRequestFactory.build("12345678", TipoDocumento.DO);
        when(usuarioDAO.findByCedula("12345678")).thenReturn(Optional.empty());

        DnicCiudadanoDTO ciudadano = new DnicCiudadanoDTO();
        ciudadano.setPrimerNombre("Lucia");
        ciudadano.setSegundoNombre("Maria");
        ciudadano.setPrimerApellido("Suarez");
        ciudadano.setSegundoApellido("Lopez");
        ciudadano.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        when(dnicClient.obtenerCiudadano("DO", "12345678")).thenReturn(ciudadano);

        when(usuarioDAO.save(any(UsuarioSalud.class))).thenAnswer(invocation -> {
            UsuarioSalud entity = invocation.getArgument(0, UsuarioSalud.class);
            entity.setId(25L);
            entity.setCreatedAt(entity.getCreatedAt());
            return entity;
        });

        UsuarioSaludDTO dto = service.registrarUsuarioEnClinica(request);

        assertEquals("12345678", dto.getCedula());
        assertEquals("Lucia", dto.getPrimerNombre());
        assertEquals("Suarez", dto.getPrimerApellido());
        assertEquals("Lucia Maria Suarez Lopez", dto.getNombreCompleto());
        assertEquals(LocalDate.of(1990, 5, 1), dto.getFechaNacimiento());
        verify(edadValidacionService).validarMayoriaDeEdad(LocalDate.of(1990, 5, 1));
        verify(usuarioDAO).save(any(UsuarioSalud.class));
    }

    @Test
    public void registrarUsuarioDevuelveExistenteSiYaEstaRegistrado() {
        UsuarioSalud existente = usuario("55566677");
        existente.setPrimerNombre("Ana");
        existente.setPrimerApellido("Test");

        when(usuarioDAO.findByCedula("55566677")).thenReturn(Optional.of(existente));

        RegistrarUsuarioRequest request = TestRegistrarUsuarioRequestFactory.build("55566677");
        UsuarioSaludDTO dto = service.registrarUsuarioEnClinica(request);

        assertEquals("55566677", dto.getCedula());
        assertEquals("Ana", dto.getPrimerNombre());
        verify(usuarioDAO, never()).save(any());
        verifyNoInteractions(dnicClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registrarUsuarioRechazaMenorDeEdad() throws Exception {
        RegistrarUsuarioRequest request = TestRegistrarUsuarioRequestFactory.build("10020030");
        when(usuarioDAO.findByCedula("10020030")).thenReturn(Optional.empty());

        DnicCiudadanoDTO ciudadano = new DnicCiudadanoDTO();
        ciudadano.setPrimerNombre("Pedro");
        ciudadano.setPrimerApellido("Joven");
        ciudadano.setFechaNacimiento(LocalDate.of(2010, 1, 1));
        when(dnicClient.obtenerCiudadano("DO", "10020030")).thenReturn(ciudadano);
        doThrow(new UsuarioMenorDeEdadException(LocalDate.of(2010, 1, 1), 14))
            .when(edadValidacionService).validarMayoriaDeEdad(LocalDate.of(2010, 1, 1));

        service.registrarUsuarioEnClinica(request);
    }

    @Test
    public void getUsuarioByCedulaRetornaDtoSiExiste() {
        UsuarioSalud usuario = usuario("77733388");
        usuario.setPrimerNombre("Mauro");
        usuario.setPrimerApellido("Gomez");
        when(usuarioDAO.findByCedula("77733388")).thenReturn(Optional.of(usuario));

        Optional<UsuarioSaludDTO> dto = service.getUsuarioByCedula("77733388");
        assertTrue(dto.isPresent());
        assertEquals("Mauro", dto.get().getPrimerNombre());
    }

    @Test
    public void actualizarUsuarioNormalizaDatos() {
        UsuarioSalud usuario = usuario("44455566");
        usuario.setPrimerNombre("Maria");
        usuario.setPrimerApellido("Viejo");
        usuario.setEmail("viejo@example.com");
        usuario.setActive(true);
        usuario.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        when(usuarioDAO.findByCedula("44455566")).thenReturn(Optional.of(usuario));
        when(usuarioDAO.save(any(UsuarioSalud.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActualizarUsuarioSaludRequest request = new ActualizarUsuarioSaludRequest();
        request.setPrimerNombre("  Lucia ");
        request.setSegundoNombre("  Maria");
        request.setPrimerApellido("Suarez   ");
        request.setSegundoApellido("   Lopez");
        request.setEmail("lucia@example.com");
        request.setTelefono(" 099123123 ");
        request.setDireccion(" Av. Siempre Viva ");
        request.setActivo(Boolean.FALSE);
        request.setNotificacionesHabilitadas(Boolean.FALSE);
        request.setFechaNacimiento(LocalDate.of(1992, 7, 10).toString());

        UsuarioSaludDTO actualizado = service.actualizarUsuario("44455566", request);

        assertEquals("Lucia", actualizado.getPrimerNombre());
        assertEquals("Maria", actualizado.getSegundoNombre());
        assertEquals("Suarez", actualizado.getPrimerApellido());
        assertEquals("Lopez", actualizado.getSegundoApellido());
        assertEquals("Lucia Maria Suarez Lopez", actualizado.getNombreCompleto());
        assertEquals("lucia@example.com", actualizado.getEmail());
        assertFalse(actualizado.getActive());
        assertEquals(LocalDate.of(1992, 7, 10), actualizado.getFechaNacimiento());
        verify(usuarioDAO).save(any(UsuarioSalud.class));
    }

    private static UsuarioSalud usuario(String cedula) {
        UsuarioSalud usuario = new UsuarioSalud();
        usuario.setCedula(cedula);
        usuario.setTipoDeDocumento(TipoDocumento.DO);
        usuario.setEmail("test@example.com");
        usuario.setActive(true);
        usuario.setNotificacionesHabilitadas(true);
        usuario.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return usuario;
    }

    private static void inject(UsuarioSaludService target, String field, Object value) throws Exception {
        Field declaredField = UsuarioSaludService.class.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);
    }
}
