package hcen.central.inus.service;

import hcen.central.inus.dao.HistoriaClinicaDAO;
import hcen.central.inus.dao.PoliticaAccesoDAO;
import hcen.central.inus.dto.PoliticaAccesoDTO;
import hcen.central.inus.entity.historia_clinica;
import hcen.central.inus.entity.politica_acceso;
import hcen.central.inus.enums.EstadoPermiso;
import hcen.central.inus.enums.TipoPermiso;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PoliticaAccesoServiceTest {

    private PoliticaAccesoService service;
    private PoliticaAccesoDAO politicaDAO;
    private HistoriaClinicaDAO historiaDAO;

    @Before
    public void setUp() throws Exception {
        service = new PoliticaAccesoService();
        politicaDAO = mock(PoliticaAccesoDAO.class);
        historiaDAO = mock(HistoriaClinicaDAO.class);
        inject("politicaDAO", politicaDAO);
        inject("historiaDAO", historiaDAO);
    }

    @Test
    public void otorgarPermisoCalculaExpiracionPorDefecto() throws Exception {
        UUID historiaId = UUID.randomUUID();
        historia_clinica historia = new historia_clinica();
        setField(historia, "id", historiaId);
        when(historiaDAO.findById(historiaId)).thenReturn(Optional.of(historia));

        UUID documento = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        PoliticaAccesoDTO dto = new PoliticaAccesoDTO();
        dto.setHistoriaClinicaId(historiaId);
        dto.setDocumentoId(documento);
        dto.setTipoPermiso(TipoPermiso.PROFESIONAL_ESPECIFICO);
        dto.setCiProfesional(12345);
        dto.setTenantId(tenant);

        ArgumentCaptor<politica_acceso> captor = ArgumentCaptor.forClass(politica_acceso.class);
        when(politicaDAO.save(any(politica_acceso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PoliticaAccesoDTO resultado = service.otorgarPermiso(dto);

        assertEquals(documento, resultado.getDocumentoId());
        verify(politicaDAO).save(captor.capture());
        politica_acceso guardada = captor.getValue();
        assertEquals(TipoPermiso.PROFESIONAL_ESPECIFICO, guardada.getTipoPermiso());
        LocalDateTime exp = guardada.getFechaExpiracion();
        LocalDateTime minimo = LocalDateTime.now().plusDays(14);
        LocalDateTime maximo = LocalDateTime.now().plusDays(16);
        assertTrue(exp.isAfter(minimo.minusMinutes(1)) && exp.isBefore(maximo.plusMinutes(1)));
        assertEquals(EstadoPermiso.ACTIVO, guardada.getEstado());
    }

    @Test(expected = IllegalStateException.class)
    public void revocarPermisoRechazaExpirados() {
        politica_acceso politica = new politica_acceso();
        politica.setFechaExpiracion(LocalDateTime.now().minusDays(1));
        politica.setEstado(EstadoPermiso.ACTIVO);
        when(politicaDAO.findById(any())).thenReturn(Optional.of(politica));

        service.revocarPermiso(UUID.randomUUID(), "expirado");
    }

    @Test
    public void validarAccesoConsultaDao() {
        UUID documento = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        when(politicaDAO.tienePermisoAcceso(documento, 1234, tenant, "CARDIO")).thenReturn(true);

        boolean tienePermiso = service.validarAcceso(documento, 1234, tenant, "CARDIO");

        assertTrue(tienePermiso);
        verify(politicaDAO).tienePermisoAcceso(documento, 1234, tenant, "CARDIO");
    }

    @Test(expected = IllegalArgumentException.class)
    public void validarAccesoRequiereDocumento() {
        service.validarAcceso(null, 1, UUID.randomUUID(), null);
    }

    private void inject(String field, Object value) throws Exception {
        Field f = PoliticaAccesoService.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(service, value);
    }

    private static void setField(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
