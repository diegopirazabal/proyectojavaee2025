package hcen.central.inus.testsupport.data;

import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.enums.TipoDocumento;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * Utilidad simple para construir {@link RegistrarUsuarioRequest} consistentes entre pruebas.
 */
public final class TestRegistrarUsuarioRequestFactory {

    private TestRegistrarUsuarioRequestFactory() {
    }

    public static RegistrarUsuarioRequest buildBasic(String cedula, UUID tenantId) {
        return build(cedula, tenantId, "Ana", "Test");
    }

    public static RegistrarUsuarioRequest build(String cedula,
                                                UUID tenantId,
                                                String primerNombre,
                                                String primerApellido) {
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest();
        request.setCedula(cedula);
        request.setTipoDocumento(TipoDocumento.DO);
        request.setPrimerNombre(primerNombre);
        request.setSegundoNombre(null);
        request.setPrimerApellido(primerApellido);
        request.setSegundoApellido(null);
        request.setEmail(formatEmail(primerNombre, primerApellido));
        request.setFechaNacimiento(LocalDate.of(1990, 1, 15));
        request.setTenantId(tenantId);
        return request;
    }

    private static String formatEmail(String nombre, String apellido) {
        String safeNombre = nombre != null ? nombre.toLowerCase(Locale.ROOT) : "test";
        String safeApellido = apellido != null ? apellido.toLowerCase(Locale.ROOT) : "example";
        return safeNombre + "." + safeApellido + "@example.com";
    }
}
