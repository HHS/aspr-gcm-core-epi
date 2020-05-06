package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import gcm.simulation.BiWeightingFunction;
import org.immutables.value.Value;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/*
    An object that provides an immutable representation of the propensities of transmission by setting and age
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableTransmissionStructure.class)
public abstract class TransmissionStructure {

    /*
        Provides the relative overall transmission likelihood by age group
     */
    @Value.Derived
    public Function<AgeGroup, Double> transmissionWeights() {
        return ageGroup -> transmissionWeightsMap().getOrDefault(ageGroup, 0.0);
    }

    public abstract Map<AgeGroup, Double> transmissionWeightsMap();

    /*
        Provides the weighting function used for biased selection of a contact group type for transmission events
     */
    @Value.Derived
    public Map<AgeGroup, Function<ContactGroupType, Double>> contactGroupSelectionWeights() {
        Map<AgeGroup, Function<ContactGroupType, Double>> contactGroupSelectionWeights =
                new LinkedHashMap<>();

        for (Map.Entry<AgeGroup, Map<ContactGroupType, Double>> entry :
                contactGroupSelectionWeightsMap().entrySet()) {
            contactGroupSelectionWeights.put(entry.getKey(),
                    contactGroupType ->
                            entry.getValue().getOrDefault(contactGroupType, 0.0));
        }

        return contactGroupSelectionWeights;
    }

    public abstract Map<AgeGroup, Map<ContactGroupType, Double>> contactGroupSelectionWeightsMap();

    /*
        Provides the bi-weighting function used for biased selection of contacts from a given group type for
            transmission events
     */
    @Value.Derived
    public Map<ContactGroupType, BiWeightingFunction> groupBiWeightingFunctions() {
        Map<ContactGroupType, BiWeightingFunction> groupBiWeightingFunctions =
                new EnumMap<>(ContactGroupType.class);

        for (Map.Entry<ContactGroupType, Map<AgeGroup, Map<AgeGroup, Double>>> entry :
                groupBiWeightingFunctionsMap().entrySet()) {
            groupBiWeightingFunctions.put(entry.getKey(),
                    (environment, sourceId, targetId, groupId) -> {
                        PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                                GlobalProperty.POPULATION_DESCRIPTION);
                        AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
                        AgeGroup sourceAgeGroup = ageGroupPartition.getAgeGroupFromIndex(
                                environment.getPersonPropertyValue(sourceId, PersonProperty.AGE_GROUP_INDEX));
                        AgeGroup targetAgeGroup = ageGroupPartition.getAgeGroupFromIndex(
                                environment.getPersonPropertyValue(targetId, PersonProperty.AGE_GROUP_INDEX));
                        // TODO: Need to handle the possibility the source age group doesn't exist in the map
                        return entry.getValue().get(sourceAgeGroup).getOrDefault(targetAgeGroup, 0.0);
                    });
        }

        return groupBiWeightingFunctions;
    }

    public abstract Map<ContactGroupType, Map<AgeGroup, Map<AgeGroup, Double>>> groupBiWeightingFunctionsMap();

    /*
        The probability that a person who lives alone will substitute global for (otherwise nonexistent) home contacts
     */
    @Value.Default
    public double singleHomeGlobalSubstitutionProbability() {
        return 0.0;
    }

}

