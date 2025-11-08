package com.hcen.periferico.service;

import com.hcen.periferico.entity.administrador_clinica;
import com.hcen.periferico.dao.AdministradorClinicaDAO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class AdministradorClinicaService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    @EJB
    private AdministradorClinicaDAO adminDAO;

    public List<administrador_clinica> listarAdministradores(UUID tenantId, String searchTerm, int page, Integer pageSize) {
        int size = normalizePageSize(pageSize);
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return adminDAO.search(searchTerm.trim(), tenantId, page, size);
        }
        if (tenantId != null) {
            return adminDAO.findByTenantPaginated(tenantId, page, size);
        }
        return adminDAO.findAllPaginated(page, size);
    }

    public long contarAdministradores(UUID tenantId, String searchTerm) {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return adminDAO.countBySearch(searchTerm.trim(), tenantId);
        }
        if (tenantId != null) {
            return adminDAO.countByTenant(tenantId);
        }
        return adminDAO.countAll();
    }

    public Optional<administrador_clinica> obtenerPorId(UUID id) {
        return adminDAO.findById(id);
    }

    public administrador_clinica actualizarAdministrador(UUID id,
                                                         String username,
                                                         String nombre,
                                                         String apellidos,
                                                         UUID tenantId) {
        administrador_clinica admin = adminDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró el administrador solicitado"));

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (apellidos == null || apellidos.trim().isEmpty()) {
            throw new IllegalArgumentException("Los apellidos son obligatorios");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenantId es obligatorio");
        }

        if (adminDAO.existsByUsernameAndTenantExcluding(username.trim(), tenantId, id)) {
            throw new IllegalArgumentException("Ya existe un administrador con ese usuario en la clínica indicada");
        }

        admin.setUsername(username.trim());
        admin.setNombre(nombre.trim());
        admin.setApellidos(apellidos.trim());
        admin.setTenantId(tenantId);

        return adminDAO.save(admin);
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
