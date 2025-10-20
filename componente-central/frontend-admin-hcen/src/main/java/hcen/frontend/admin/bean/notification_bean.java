package hcen.frontend.admin.bean;

import hcen.frontend.admin.service.api_service;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@RequestScoped
public class notification_bean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private api_service apiService;

    public String sendTestNotification() {
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            boolean success = apiService.triggerBroadcastNotification();

            if (success) {
                context.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Notificación enviada",
                        "Se envió la notificación de prueba a todos los usuarios."));
            } else {
                context.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Notificación no enviada",
                        "No se pudo contactar al backend para enviar la notificación."));
            }
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Error",
                    "Ocurrió un error al enviar la notificación de prueba."));
        }

        return null;
    }
}
