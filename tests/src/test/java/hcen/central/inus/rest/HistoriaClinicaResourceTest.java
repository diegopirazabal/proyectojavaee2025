package hcen.central.inus.rest;

import hcen.central.inus.dto.HistoriaClinicaDocumentoDetalleResponse;
import hcen.central.inus.service.HistoriaClinicaService;
import hcen.central.notifications.dto.ApiResponse;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HistoriaClinicaResourceTest {

    private HistoriaClinicaResource resource;
    private HistoriaClinicaService service;

    @Before
    public void setUp() throws Exception {
        resource = new HistoriaClinicaResource();
        service = mock(HistoriaClinicaService.class);
        Field field = HistoriaClinicaResource.class.getDeclaredField("historiaClinicaService");
        field.setAccessible(true);
        field.set(resource, service);
    }

    @Test
    public void registrarDocumentoRetornaCreated() {
        UUID historiaId = UUID.randomUUID();
        when(service.registrarDocumento(any(), any(), any())).thenReturn(historiaId);

        HistoriaClinicaResource.RegistrarDocumentoRequest request = new HistoriaClinicaResource.RegistrarDocumentoRequest();
        request.setCedula("123");
        request.setDocumentoId(UUID.randomUUID().toString());
        request.setTenantId(UUID.randomUUID().toString());

        Response response = resource.registrarDocumento(request);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        HistoriaClinicaResource.RegistrarDocumentoResponse body =
            (HistoriaClinicaResource.RegistrarDocumentoResponse) response.getEntity();
        assertEquals(historiaId.toString(), body.getHistoriaId());
    }

    @Test
    public void registrarDocumentoPropagaErroresDeValidacion() {
        when(service.registrarDocumento(any(), any(), any()))
            .thenThrow(new IllegalArgumentException("error"));

        HistoriaClinicaResource.RegistrarDocumentoRequest request = new HistoriaClinicaResource.RegistrarDocumentoRequest();
        request.setCedula("123");
        request.setDocumentoId(UUID.randomUUID().toString());
        request.setTenantId(UUID.randomUUID().toString());

        Response response = resource.registrarDocumento(request);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        HistoriaClinicaResource.ErrorResponse body = (HistoriaClinicaResource.ErrorResponse) response.getEntity();
        assertTrue(body.getError().contains("error"));
    }

    @Test
    public void registrarDocumentoRechazaRequestInvalidoAntesDelServicio() {
        Response response = resource.registrarDocumento(new HistoriaClinicaResource.RegistrarDocumentoRequest());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verify(service, never()).registrarDocumento(any(), any(), any());
    }

    @Test
    public void obtenerDocumentosRetornaApiResponse() {
        HistoriaClinicaDocumentoDetalleResponse dto = new HistoriaClinicaDocumentoDetalleResponse();
        dto.setDocumentoId("doc");
        when(service.obtenerDocumentosPorCedula("123")).thenReturn(List.of(dto));

        Response response = resource.obtenerDocumentosPorCedula("123");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        ApiResponse<List<HistoriaClinicaDocumentoDetalleResponse>> api =
            (ApiResponse<List<HistoriaClinicaDocumentoDetalleResponse>>) response.getEntity();
        assertTrue(api.isSuccess());
        assertEquals("doc", api.getData().get(0).getDocumentoId());
    }
}
