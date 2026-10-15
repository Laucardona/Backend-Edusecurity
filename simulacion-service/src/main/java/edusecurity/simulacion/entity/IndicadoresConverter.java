package edusecurity.simulacion.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Guarda el conjunto de señales marcadas como texto separado por comas. */
@Converter
public class IndicadoresConverter implements AttributeConverter<Set<TipoIndicador>, String> {

    @Override
    public String convertToDatabaseColumn(Set<TipoIndicador> indicadores) {
        if (indicadores == null || indicadores.isEmpty()) {
            return "";
        }
        return indicadores.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    @Override
    public Set<TipoIndicador> convertToEntityAttribute(String valor) {
        if (valor == null || valor.isBlank()) {
            return EnumSet.noneOf(TipoIndicador.class);
        }
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(TipoIndicador::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoIndicador.class)));
    }
}
