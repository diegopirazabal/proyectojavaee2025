package com.hcen.mockdnic.ws;

import com.hcen.mockdnic.dominio.Ciudadano;
import com.hcen.mockdnic.dominio.ciudadano_id;
import com.hcen.mockdnic.excepciones.ciudadano_no_encontrado_exception;
import com.hcen.mockdnic.ws.dto.respuesta_ciudadano;
import com.hcen.mockdnic.ws.dto.solicitud_ciudadano;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.jws.soap.SOAPBinding;

@Stateless
@WebService(
        serviceName = "ciudadano-service",
        portName = "ciudadano_port",
        targetNamespace = "http://hcen.com/soap/ciudadano",
        wsdlLocation = "WEB-INF/wsdl/ciudadano-service.wsdl"
)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class ciudadano_servicio_web {

    @PersistenceContext(unitName = "mock-dnic-pu")
    private EntityManager entidad;

    @WebMethod(operationName = "obtener_ciudadano")
    @WebResult(
            name = "respuesta_ciudadano",
            targetNamespace = "http://hcen.com/soap/ciudadano",
            partName = "respuesta_ciudadano"
    )
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public respuesta_ciudadano obtener_ciudadano(
            @WebParam(
                    name = "solicitud_ciudadano",
                    targetNamespace = "http://hcen.com/soap/ciudadano",
                    partName = "solicitud_ciudadano"
            )
            solicitud_ciudadano solicitud) throws ciudadano_no_encontrado_exception {

        ciudadano_id id = new ciudadano_id(solicitud.getTipoDocumento(), solicitud.getNumeroDocumento());
        Ciudadano ciudadano = entidad.find(Ciudadano.class, id);
        if (ciudadano == null) {
            throw new ciudadano_no_encontrado_exception(solicitud.getTipoDocumento(), solicitud.getNumeroDocumento());
        }
        return convertir(ciudadano);
    }

    private respuesta_ciudadano convertir(Ciudadano ciudadano) {
        respuesta_ciudadano respuesta = new respuesta_ciudadano();
        respuesta.setTipoDocumento(ciudadano.getTipoDocumento());
        respuesta.setNumeroDocumento(ciudadano.getNumeroDocumento());
        respuesta.setPrimerNombre(ciudadano.getPrimerNombre());
        respuesta.setSegundoNombre(ciudadano.getSegundoNombre());
        respuesta.setPrimerApellido(ciudadano.getPrimerApellido());
        respuesta.setSegundoApellido(ciudadano.getSegundoApellido());
        respuesta.setSexo(ciudadano.getSexo());
        respuesta.setFechaNacimiento(ciudadano.getFechaNacimiento().toString());
        respuesta.setCodigoNacionalidad(ciudadano.getCodigoNacionalidad());
        respuesta.setNombreEnCedula(ciudadano.getNombreEnCedula());
        return respuesta;
    }
}
