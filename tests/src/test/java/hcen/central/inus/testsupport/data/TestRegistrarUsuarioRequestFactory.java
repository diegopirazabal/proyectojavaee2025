package hcen.central.inus.testsupport.data;

import hcen.central.inus.dto.RegistrarUsuarioRequest;
import hcen.central.inus.enums.TipoDocumento;

/**
 * Utilidad simple para construir {@link RegistrarUsuarioRequest} consistentes entre pruebas.
 */
public final class TestRegistrarUsuarioRequestFactory {

    private TestRegistrarUsuarioRequestFactory() { }

    public static RegistrarUsuarioRequest build(String cedula) {
        return build(cedula, TipoDocumento.DO);
    }

    public static RegistrarUsuarioRequest build(String cedula, TipoDocumento tipoDocumento) {
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest();
        request.setCedula(cedula);
        request.setTipoDocumento(tipoDocumento);
        return request;
    }
}
