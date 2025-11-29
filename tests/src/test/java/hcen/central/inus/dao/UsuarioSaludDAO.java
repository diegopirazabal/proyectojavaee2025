package hcen.central.inus.dao;

import hcen.central.inus.entity.UsuarioSalud;
import hcen.central.inus.enums.TipoDocumento;
import jakarta.ejb.Stateless;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DAO en memoria para los IT, evita JPA/OpenJPA.
 */
@Stateless
public class UsuarioSaludDAO {

    private final ConcurrentHashMap<String, UsuarioSalud> usuarios = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    public UsuarioSalud save(UsuarioSalud usuario) {
        if (usuario.getId() == null) {
            usuario.setId(seq.getAndIncrement());
            if (usuario.getCreatedAt() == null) {
                usuario.setCreatedAt(LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC));
            }
        }
        usuarios.put(usuario.getCedula(), usuario);
        return usuario;
    }

    public Optional<UsuarioSalud> findByCedula(String cedula) {
        return Optional.ofNullable(usuarios.get(cedula));
    }

    public boolean existsByCedula(String cedula) {
        return usuarios.containsKey(cedula);
    }

    public List<UsuarioSalud> findAllActive() {
        List<UsuarioSalud> list = new ArrayList<>();
        for (UsuarioSalud u : usuarios.values()) {
            if (Boolean.TRUE.equals(u.getActive())) {
                list.add(u);
            }
        }
        return list;
    }

    // Métodos no usados en el IT se dejan sin implementar o con defaults
    public List<UsuarioSalud> findAllPaginated(int page, int size) { return findAllActive(); }
    public List<UsuarioSalud> findByFilters(TipoDocumento tipoDocumento, String numeroDocumento, String nombre, String apellido, int page, int size) { return findAllActive(); }
    public long countByFilters(TipoDocumento tipoDocumento, String numeroDocumento, String nombre, String apellido) { return usuarios.size(); }
    public long countAllActive() { return usuarios.size(); }
    public List<UsuarioSalud> searchByNombreOrApellido(String term) { return findAllActive(); }
    public void deactivate(String cedula) { Optional.ofNullable(usuarios.get(cedula)).ifPresent(u -> u.setActive(false)); }
    public Optional<UsuarioSalud> findById(Long id) { return usuarios.values().stream().filter(u -> id.equals(u.getId())).findFirst(); }
}
