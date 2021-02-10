package gcm.core.epi.variants;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Value.Immutable
@JsonDeserialize(as = ImmutableVariantsDescription.class)
public abstract class VariantsDescription {

    private static final VariantId REFERENCE_STRAIN = VariantId.REFERENCE_ID;
    private static final VariantDefinition REFERENCE_VARIANT_DEFINITION = ImmutableVariantDefinition.builder().build();

    abstract Map<VariantId, VariantDefinition> variantDefinitions();

    abstract Map<VariantId, Map<VariantId, Double>> priorInfectionImmunity();

    @Value.Derived
    public List<VariantId> variantIdList() {
        List<VariantId> variantIdList = new ArrayList<>();
        // Reference strain will always be first
        variantIdList.add(REFERENCE_STRAIN);
        variantDefinitions().forEach((variantId, VariantDefinition) -> variantIdList.add(variantId));
        return variantIdList;
    }

    @Value.Derived
    Map<VariantId, Integer> variantIdIndex() {
        Map<VariantId, Integer> variantIdIndex = new HashMap<>();
        AtomicInteger counter = new AtomicInteger();
        variantIdList().forEach(variantId -> {
            variantIdIndex.put(variantId, counter.getAndIncrement());
        });
        return variantIdIndex;
    }

    public double getInfectionProbability(int sourceIndex, int targetIndex) {
        if (targetIndex < 0) {
            return 1.0;
        }
        VariantId sourceVariantId = variantIdList().get(sourceIndex);
        VariantId targetVariantId = variantIdList().get(targetIndex);
        if (priorInfectionImmunity().containsKey(sourceVariantId)) {
            Map<VariantId, Double> priorInfectionImmunity = priorInfectionImmunity().get(sourceVariantId);
            return 1.0 - priorInfectionImmunity.getOrDefault(targetVariantId, 1.0);
        } else {
            return 0.0;
        }
    }

    public VariantDefinition getVariantDefinition(int variantIndex) {
        if (variantIndex > 0) {
            return variantDefinitions().get(variantIdList().get(variantIndex));
        } else {
            return REFERENCE_VARIANT_DEFINITION;
        }
    }

    public int getVariantIndex(VariantId variantId) {
        if (variantIdIndex().containsKey(variantId)) {
            return variantIdIndex().get(variantId);
        } else {
            throw new RuntimeException("Invalid variant id: " + variantId.toString());
        }
    }

}
