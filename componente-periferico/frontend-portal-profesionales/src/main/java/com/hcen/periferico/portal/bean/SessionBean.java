package com.hcen.periferico.portal.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class SessionBean implements Serializable {
    private Integer ci; private String nombre; private String apellidos; private String tenantId;
    public Integer getCi(){return ci;} public void setCi(Integer v){ci=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getApellidos(){return apellidos;} public void setApellidos(String v){apellidos=v;}
    public String getTenantId(){return tenantId;} public void setTenantId(String v){tenantId=v;}
    public String getNombreCompleto(){ return (nombre!=null?nombre:"")+" "+(apellidos!=null?apellidos:"").trim(); }
}
