package gcm.core.epi.util.property;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import gcm.scenario.MapOption;
import gcm.scenario.PropertyDefinition;
import gcm.scenario.TimeTrackingPolicy;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Value.Immutable
public abstract class TypedPropertyDefinition {

    public static ImmutableTypedPropertyDefinition.Builder builder() {
        return ImmutableTypedPropertyDefinition.builder();
    }

    abstract Optional<TypeReference<?>> typeReference();

    abstract Optional<Class<?>> type();

    @Value.Derived
    public JavaType javaType() {
        if (typeReference().isPresent()) {
            return TypeFactory.defaultInstance().constructType(typeReference().get());
        } else {
            return TypeFactory.defaultInstance().constructSimpleType(type().get(), null);
        }
    }

    abstract @Nullable Object defaultValue();

    @Value.Default
    MapOption mapOption() {
        return MapOption.NONE;
    }

    @Value.Default
    TimeTrackingPolicy timeTrackingPolicy() {
        return TimeTrackingPolicy.DO_NOT_TRACK_TIME;
    }

    @Value.Default
    boolean isMutable() {
        return true;
    }

    @Value.Derived
    public PropertyDefinition definition() {

        // Can set all fields aside from
        PropertyDefinition.Builder builder = PropertyDefinition.builder()
                .setType(javaType().getRawClass())
                .setMapOption(mapOption())
                .setTimeTrackingPolicy(timeTrackingPolicy())
                .setPropertyValueMutability(isMutable());

        if (defaultValue() != null) {
            builder.setDefaultValue(defaultValue());
        }

        return builder.build();
    }

}
