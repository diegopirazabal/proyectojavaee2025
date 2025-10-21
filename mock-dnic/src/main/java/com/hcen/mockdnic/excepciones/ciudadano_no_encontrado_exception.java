package com.hcen.mockdnic.excepciones;

import com.hcen.mockdnic.ws.dto.ciudadano_no_encontrado_fault;
import jakarta.xml.ws.WebFault;
import java.util.Objects;

@WebFault(
        name = "ciudadano_no_encontrado",
        targetNamespace = "http://hcen.com/soap/ciudadano",
        faultBean = "com.hcen.mockdnic.ws.dto.ciudadano_no_encontrado_fault"
)
public class ciudadano_no_encontrado_exception extends Exception {

    private final ciudadano_no_encontrado_fault faultInfo;

    public ciudadano_no_encontrado_exception(String tipoDocumento, String numeroDocumento) {
        super(String.format("No se encontró ciudadano con documento %s %s", tipoDocumento, numeroDocumento));
        this.faultInfo = new ciudadano_no_encontrado_fault(tipoDocumento, numeroDocumento);
    }

    public ciudadano_no_encontrado_exception(String tipoDocumento, String numeroDocumento, Throwable cause) {
        super(String.format("No se encontró ciudadano con documento %s %s", tipoDocumento, numeroDocumento), cause);
        this.faultInfo = new ciudadano_no_encontrado_fault(tipoDocumento, numeroDocumento);
    }

    public ciudadano_no_encontrado_exception(String message, ciudadano_no_encontrado_fault faultInfo) {
        super(message);
        this.faultInfo = Objects.requireNonNull(faultInfo, "faultInfo");
    }

    public ciudadano_no_encontrado_exception(String message, ciudadano_no_encontrado_fault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = Objects.requireNonNull(faultInfo, "faultInfo");
    }

    public ciudadano_no_encontrado_fault getFaultInfo() {
        return faultInfo;
    }

    public String getTipoDocumento() {
        return faultInfo != null ? faultInfo.getTipoDocumento() : null;
    }

    public String getNumeroDocumento() {
        return faultInfo != null ? faultInfo.getNumeroDocumento() : null;
    }
}
