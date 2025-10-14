package com.hcen.mockdnic.ws;

import com.hcen.mockdnic.dominio.Ciudadano;
import com.hcen.mockdnic.dominio.CiudadanoId;
import com.hcen.mockdnic.excepciones.CiudadanoNoEncontradoException;
import com.hcen.mockdnic.ws.dto.RespuestaCiudadano;
import com.hcen.mockdnic.ws.dto.SolicitudCiudadano;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
@WebService(
        serviceName = "ciudadano-service",
        portName = "CiudadanoPort",
        targetNamespace = "http://hcen.com/soap/ciudadano",
        wsdlLocation = "WEB-INF/wsdl/ciudadano-service.wsdl"
)
public class CiudadanoServicioWeb {

    @PersistenceContext(unitName = "mock-dnic-pu")
    private EntityManager entidad;

    @WebMethod(operationName = "obtenerCiudadano")
    @WebResult(name = "RespuestaCiudadano", targetNamespace = "http://hcen.com/soap/ciudadano")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public RespuestaCiudadano obtenerCiudadano(
            @WebParam(name = "SolicitudCiudadano", targetNamespace = "http://hcen.com/soap/ciudadano")
            SolicitudCiudadano solicitud) throws CiudadanoNoEncontradoException {

        CiudadanoId id = new CiudadanoId(solicitud.getTipoDocumento(), solicitud.getNumeroDocumento());
        Ciudadano ciudadano = entidad.find(Ciudadano.class, id);
        if (ciudadano == null) {
            throw new CiudadanoNoEncontradoException(solicitud.getTipoDocumento(), solicitud.getNumeroDocumento());
        }
        return convertir(ciudadano);
    }

    private RespuestaCiudadano convertir(Ciudadano ciudadano) {
        RespuestaCiudadano respuesta = new RespuestaCiudadano();
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
