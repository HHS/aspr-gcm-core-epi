package gcm.core.epi.trigger;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.components.Component;
import gcm.core.epi.components.trigger.IncidenceTriggerComponent;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.AbstractFipsCodeDouble;
import gcm.core.epi.propertytypes.FipsCode;
import gcm.core.epi.propertytypes.FipsScope;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toMap;

@Value.Immutable
@JsonDeserialize(as = ImmutableIncidenceTrigger.class)
public abstract class IncidenceTrigger extends AbstractFipsCodeDouble implements Trigger {

    @Value.Default
    public FipsScope scope() {
        return FipsScope.NATION;
    }

    public abstract Metric metric();

    public abstract double interval();

    public abstract Comparison comparison();

    @Value.Default
    public double start() {
        return 0.0;
    }

    @Value.Default
    public double end() {
        return Double.POSITIVE_INFINITY;
    }

    public abstract Map<FipsCode, Double> cutoffs();

    @Value.Default
    public Double defaultCutoff() {
        return comparison() == Comparison.BELOW ? 0.0 : Double.POSITIVE_INFINITY;
    }

    public Map<FipsCode, Double> getFipsCodeCutoffs(Environment environment) {
        Map<FipsCode, Double> thresholdsByFipsCode;
        PopulationDescription populationDescription = environment.getGlobalPropertyValue(GlobalProperty.POPULATION_DESCRIPTION);
        if (type() == ValueType.NUMBER) {
            Set<FipsCode> fipsCodes = populationDescription.regionIds().stream()
                    .map(scope()::getFipsSubCode)
                    .collect(Collectors.toSet());
            thresholdsByFipsCode = fipsCodes.stream()
                    .collect(toMap(fipsCode -> fipsCode, fipsCode -> cutoffs().getOrDefault(fipsCode, defaultCutoff())));
        } else {
            Map<RegionId, Long> regionPopulations = populationDescription.populationByRegion();
            Map<FipsCode, Long> fipsCodePopulations = populationDescription.regionIds().stream()
                    .collect(Collectors.groupingBy(scope()::getFipsSubCode,
                            summingLong(regionId -> regionPopulations.getOrDefault(regionId, 0L))));
            thresholdsByFipsCode = fipsCodePopulations.entrySet().stream()
                    .collect(toMap(Map.Entry::getKey,
                            entry -> entry.getValue().doubleValue() *
                                    cutoffs().getOrDefault(entry.getKey(), defaultCutoff())));
        }
        return thresholdsByFipsCode;
    }

    public Class<? extends Component> triggerComponent() {
        return IncidenceTriggerComponent.class;
    }

    public enum Metric {
        INFECTIONS,
        CASES,
        HOSPITALIZATIONS,
        DEATHS
    }

    public enum Comparison {
        BELOW,
        ABOVE
    }

}
