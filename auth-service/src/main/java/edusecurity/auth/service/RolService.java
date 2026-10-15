package edusecurity.auth.service;

import edusecurity.auth.dto.RolRequest;
import edusecurity.auth.dto.RolResponse;
import edusecurity.auth.entity.Rol;
import edusecurity.auth.repository.RolRepository;
import edusecurity.common.exception.ConflictException;
import edusecurity.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolService {

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listar() {
        return rolRepository.findAll().stream().map(RolResponse::from).toList();
    }

    @Transactional
    public RolResponse crear(RolRequest request) {
        String nombre = request.nombre().trim().toUpperCase();

        if (rolRepository.existsByNombre(nombre)) {
            throw new ConflictException("El rol ya existe: " + nombre);
        }

        Rol rol = new Rol(nombre, request.descripcion());
        return RolResponse.from(rolRepository.save(rol));
    }

    @Transactional(readOnly = true)
    public RolResponse obtener(Long id) {
        return rolRepository.findById(id)
                .map(RolResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", id));
    }
}
