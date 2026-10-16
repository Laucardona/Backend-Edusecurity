package edusecurity.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** Conjunto completo de permisos que tendrá el rol (reemplaza los actuales). */
public record PermisosRequest(@NotNull @Size(max = 100) Set<String> permisos) {
}
