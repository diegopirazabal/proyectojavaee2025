package hcen.central.inus.rest;

import hcen.central.inus.dto.ActualizarUsuarioSaludRequest;
import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.dto.UsuarioSaludDTO;
import hcen.central.inus.enums.TipoDocumento;
import hcen.central.inus.service.UsuarioSaludService;
import hcen.central.inus.testsupport.data.TestRegistrarUsuarioRequestFactory;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link UsuarioSaludResource}.
 * Se validan códigos HTTP y el manejo correcto de errores en la capa REST.
 */
public class UsuarioSaludResourceTest {

    private UsuarioSaludResource resource;
    private UsuarioSaludService usuarioSaludService;

    @Before
    public void setUp() throws Exception {
        resource = new UsuarioSaludResource();
        usuarioSaludService = mock(UsuarioSaludService.class);
        Field field = UsuarioSaludResource.class.getDeclaredField("usuarioService");
        field.setAccessible(true);
        field.set(resource, usuarioSaludService);
    }

    @Test
    public void verificarUsuarioExisteRetornaOk() {
        when(usuarioSaludService.verificarUsuarioExiste("12345678")).thenReturn(true);

        Response response = resource.verificarUsuarioExiste("12345678");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertEquals(Boolean.TRUE, body.get("existe"));
        assertEquals("12345678", body.get("cedula"));
    }

    @Test
    public void verificarUsuarioExistePropagaErroresDeValidacion() {
        when(usuarioSaludService.verificarUsuarioExiste("")).thenThrow(new IllegalArgumentException("cedula"));

        Response response = resource.verificarUsuarioExiste("");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void registrarUsuarioRetornaDto() {
        RegistrarUsuarioRequest request = TestRegistrarUsuarioRequestFactory.build("11112223", TipoDocumento.DO);
        UsuarioSaludDTO dto = new UsuarioSaludDTO();
        dto.setCedula("11112223");
        dto.setPrimerNombre("Lucia");
        when(usuarioSaludService.registrarUsuarioEnClinica(request)).thenReturn(dto);

        Response response = resource.registrarUsuario(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(dto, response.getEntity());
    }

    @Test
    public void registrarUsuarioPropagaErroresDelServicio() {
        RegistrarUsuarioRequest request = TestRegistrarUsuarioRequestFactory.build("99988877", TipoDocumento.DO);
        when(usuarioSaludService.registrarUsuarioEnClinica(request))
            .thenThrow(new IllegalArgumentException("error"));

        Response response = resource.registrarUsuario(request);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getUsuarioByCedulaRetornaNotFoundCuandoNoExiste() {
        when(usuarioSaludService.getUsuarioByCedula("00000000")).thenReturn(Optional.empty());

        Response response = resource.getUsuarioByCedula("00000000");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void getUsuarioByCedulaRetornaDatos() {
        UsuarioSaludDTO dto = new UsuarioSaludDTO();
        dto.setCedula("22223333");
        dto.setPrimerNombre("Ana");
        when(usuarioSaludService.getUsuarioByCedula("22223333")).thenReturn(Optional.of(dto));

        Response response = resource.getUsuarioByCedula("22223333");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(dto, response.getEntity());
    }

    @Test
    public void getUsuarioByCedulaValidaEntrada() {
        when(usuarioSaludService.getUsuarioByCedula(" "))
            .thenThrow(new IllegalArgumentException("cedula requerida"));

        Response response = resource.getUsuarioByCedula(" ");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void actualizarUsuarioRetornaDtoActualizado() {
        ActualizarUsuarioSaludRequest request = new ActualizarUsuarioSaludRequest();
        request.setPrimerNombre("Pablo");
        request.setPrimerApellido("Diaz");
        request.setFechaNacimiento(LocalDate.of(1995, 6, 20).toString());

        UsuarioSaludDTO dto = new UsuarioSaludDTO();
        dto.setCedula("33339999");
        dto.setPrimerNombre("Pablo");
        dto.setPrimerApellido("Diaz");
        when(usuarioSaludService.actualizarUsuario("33339999", request)).thenReturn(dto);

        Response response = resource.actualizarUsuario("33339999", request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(dto, response.getEntity());
        verify(usuarioSaludService).actualizarUsuario("33339999", request);
    }

    @Test
    public void actualizarUsuarioPropagaBadRequest() {
        ActualizarUsuarioSaludRequest request = new ActualizarUsuarioSaludRequest();
        when(usuarioSaludService.actualizarUsuario("1234", request))
            .thenThrow(new IllegalArgumentException("error"));

        Response response = resource.actualizarUsuario("1234", request);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("error"));
    }
}
