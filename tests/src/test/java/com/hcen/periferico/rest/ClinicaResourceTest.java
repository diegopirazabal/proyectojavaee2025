package com.hcen.periferico.rest;

import com.hcen.periferico.dao.ClinicaDAO;
import com.hcen.periferico.entity.clinica;
import com.hcen.periferico.service.ClinicaService;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClinicaResourceTest {

    private ClinicaResource resource;
    private ClinicaDAO clinicaDAO;
    private ClinicaService clinicaService;

    @Before
    public void setUp() throws Exception {
        resource = new ClinicaResource();
        clinicaDAO = mock(ClinicaDAO.class);
        clinicaService = mock(ClinicaService.class);
        inject(resource, "clinicaDAO", clinicaDAO);
        inject(resource, "clinicaService", clinicaService);
    }

    @Test
    public void createClinicaExitoso() throws Exception {
        ClinicaResource.ClinicaCreateRequest request = new ClinicaResource.ClinicaCreateRequest();
        request.setNombre("Nueva Clinica");
        request.setDireccion("Calle 123");
        request.setEmail("info@clinica.com");

        when(clinicaDAO.findByNombreIgnoreCase("Nueva Clinica")).thenReturn(Optional.empty());

        clinica creada = new clinica();
        creada.setTenantId(UUID.randomUUID());
        creada.setNombre("Nueva Clinica");
        creada.setDireccion("Calle 123");
        creada.setEmail("info@clinica.com");
        creada.setEstado("ACTIVA");
        creada.setFecRegistro(LocalDateTime.of(2024, 1, 1, 10, 0));

        when(clinicaService.crearClinicaConAdministrador("Nueva Clinica", "Calle 123", "info@clinica.com"))
            .thenReturn(creada);

        Response response = resource.createClinica(request);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        ClinicaResource.ClinicaDTO dto = (ClinicaResource.ClinicaDTO) response.getEntity();
        assertEquals(creada.getTenantId().toString(), dto.getTenantId());
        assertEquals("Nueva Clinica", dto.getNombre());
        assertEquals("Calle 123", dto.getDireccion());
        assertEquals("info@clinica.com", dto.getEmail());
        assertEquals("ACTIVA", dto.getEstado());
        assertNotNull(dto.getFecRegistro());
        verify(clinicaService).crearClinicaConAdministrador("Nueva Clinica", "Calle 123", "info@clinica.com");
    }

    @Test
    public void createClinicaValidaNombreObligatorio() {
        Response response = resource.createClinica(new ClinicaResource.ClinicaCreateRequest());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(clinicaService);
    }

    @Test
    public void createClinicaRechazaDuplicados() {
        ClinicaResource.ClinicaCreateRequest request = new ClinicaResource.ClinicaCreateRequest();
        request.setNombre("Existente");

        when(clinicaDAO.findByNombreIgnoreCase("Existente")).thenReturn(Optional.of(new clinica()));

        Response response = resource.createClinica(request);

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        verifyNoInteractions(clinicaService);
    }

    @Test
    public void getAllClinicasMapeaDtos() {
        clinica c1 = new clinica();
        c1.setTenantId(UUID.randomUUID());
        c1.setNombre("Clinica 1");
        clinica c2 = new clinica();
        c2.setTenantId(UUID.randomUUID());
        c2.setNombre("Clinica 2");

        when(clinicaDAO.findAll()).thenReturn(List.of(c1, c2));

        Response response = resource.getAllClinicas();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        List<ClinicaResource.ClinicaDTO> dtos = (List<ClinicaResource.ClinicaDTO>) response.getEntity();
        assertEquals(2, dtos.size());
        assertEquals("Clinica 1", dtos.get(0).getNombre());
        assertEquals("Clinica 2", dtos.get(1).getNombre());
        verify(clinicaDAO).findAll();
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
