package com.hcen.periferico.service;

import com.hcen.periferico.dao.ClinicaDAO;
import com.hcen.periferico.entity.clinica;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClinicaServiceTest {

    private ClinicaService service;
    private ClinicaDAO clinicaDAO;
    private AuthenticationService authenticationService;

    @Before
    public void setUp() throws Exception {
        service = new ClinicaService();
        clinicaDAO = mock(ClinicaDAO.class);
        authenticationService = mock(AuthenticationService.class);
        inject(service, "clinicaDAO", clinicaDAO);
        inject(service, "authenticationService", authenticationService);
    }

    @Test
    public void creaClinicaConEstadoActivaYGeneraAdmin() throws Exception {
        when(clinicaDAO.save(any(clinica.class))).thenAnswer(invocation -> {
            clinica c = invocation.getArgument(0, clinica.class);
            if (c.getTenantId() == null) {
                c.setTenantId(UUID.randomUUID());
            }
            if (c.getFecRegistro() == null) {
                c.setFecRegistro(LocalDateTime.now());
            }
            return c;
        });

        clinica creada = service.crearClinicaConAdministrador("Clinica Demo", "Dir", "info@demo.com");

        assertEquals("ACTIVA", creada.getEstado());
        assertNotNull(creada.getTenantId());
        assertNotNull(creada.getFecRegistro());

        ArgumentCaptor<clinica> captor = ArgumentCaptor.forClass(clinica.class);
        verify(clinicaDAO).save(captor.capture());
        clinica guardada = captor.getValue();
        assertEquals("Clinica Demo", guardada.getNombre());
        verify(authenticationService).createAdmin(startsWith("administrador-"),
            eq("1234"), any(), any(), eq(guardada.getTenantId()), eq(false));
    }

    @Test
    public void generaUsernameCuandoNombreVacio() throws Exception {
        UUID fixedTenant = UUID.randomUUID();
        when(clinicaDAO.save(any(clinica.class))).thenAnswer(invocation -> {
            clinica c = invocation.getArgument(0, clinica.class);
            c.setTenantId(fixedTenant);
            return c;
        });

        clinica creada = service.crearClinicaConAdministrador("   ", null, null);

        assertEquals("ACTIVA", creada.getEstado());
        verify(authenticationService).createAdmin(
            startsWith("administrador-"), eq("1234"), any(), any(), eq(fixedTenant), eq(false));
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
