package hcen.frontend.profesionales.bean;

import hcen.frontend.profesionales.dto.SolicitudDTO;
import hcen.frontend.profesionales.service.APIService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

@Named
@RequestScoped
public class SolicitudBean {
    @Inject
    private APIService api;

    public List<SolicitudDTO> getSolicitudes() {
        return api.listarSolicitudes();
    }

    public void reintentar(SolicitudDTO s) {
        api.reintentarSolicitud(s.getId());
    }
}
