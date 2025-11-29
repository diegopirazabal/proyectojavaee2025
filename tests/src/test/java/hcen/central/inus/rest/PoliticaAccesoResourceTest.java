package hcen.central.inus.rest;

import hcen.central.inus.dto.PoliticaAccesoDTO;
import hcen.central.inus.enums.TipoPermiso;
import hcen.central.inus.service.PoliticaAccesoService;
import hcen.central.notifications.dto.ApiResponse;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PoliticaAccesoResourceTest {

    private PoliticaAccesoResource resource;
    private PoliticaAccesoService service;

    @Before
    public void setUp() throws Exception {
        resource = new PoliticaAccesoResource();
        service = mock(PoliticaAccesoService.class);
        Field field = PoliticaAccesoResource.class.getDeclaredField("politicaService");
        field.setAccessible(true);
        field.set(resource, service);
    }

    @Test
    public void otorgarPermisoExitoso() {
        PoliticaAccesoDTO dto = new PoliticaAccesoDTO();
        dto.setId(UUID.randomUUID());
        when(service.otorgarPermiso(any())).thenReturn(dto);

        PoliticaAccesoResource.OtorgarPermisoRequest request = new PoliticaAccesoResource.OtorgarPermisoRequest();
        request.setHistoriaClinicaId(UUID.randomUUID().toString());
        request.setDocumentoId(UUID.randomUUID().toString());
        request.setTipoPermiso(TipoPermiso.POR_CLINICA.name());
        request.setTenantId(UUID.randomUUID().toString());

        Response response = resource.otorgarPermiso(request);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        ApiResponse<PoliticaAccesoDTO> api = (ApiResponse<PoliticaAccesoDTO>) response.getEntity();
        assertTrue(api.isSuccess());
        assertEquals(dto.getId(), api.getData().getId());
    }

    @Test
    public void otorgarPermisoValidaCampos() {
        PoliticaAccesoResource.OtorgarPermisoRequest request = new PoliticaAccesoResource.OtorgarPermisoRequest();
        Response response = resource.otorgarPermiso(request);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verify(service, never()).otorgarPermiso(any());
    }

    @Test
    public void validarAccesoRetornaResultado() {
        UUID documentoId = UUID.randomUUID();
        when(service.validarAccesoBatch(anyList(), anyInt(), any(), any())).thenReturn(Map.of(documentoId, true));

        PoliticaAccesoResource.ValidarAccesoRequest request = new PoliticaAccesoResource.ValidarAccesoRequest();
        request.setDocumentoIds(List.of(documentoId.toString()));
        request.setTenantId(UUID.randomUUID().toString());
        request.setCiProfesional(123);

        Response response = resource.validarAcceso(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        ApiResponse<PoliticaAccesoResource.ValidarAccesoResponse> api =
            (ApiResponse<PoliticaAccesoResource.ValidarAccesoResponse>) response.getEntity();
        assertEquals(Boolean.TRUE, api.getData().getPermisos().get(documentoId.toString()));
    }

    @Test
    public void listarPermisosPacienteDevuelveDatos() {
        PoliticaAccesoDTO dto = new PoliticaAccesoDTO();
        dto.setFechaExpiracion(LocalDateTime.now());
        when(service.listarPermisosPaciente(any())).thenReturn(List.of(dto));

        Response response = resource.listarPermisosPaciente(UUID.randomUUID().toString());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        ApiResponse<List<PoliticaAccesoDTO>> api = (ApiResponse<List<PoliticaAccesoDTO>>) response.getEntity();
        assertEquals(1, api.getData().size());
    }

    @Test
    public void revocarPermisoPropagaErroresDeNegocio() {
        doThrow(new IllegalStateException("ya revocado")).when(service).revocarPermiso(any(), any());

        Response response = resource.revocarPermiso(UUID.randomUUID().toString(), new PoliticaAccesoResource.RevocarPermisoRequest());
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    }
}
