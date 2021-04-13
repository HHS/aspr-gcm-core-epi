package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.identifiers.ContactGroupType;
import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import org.immutables.value.Value;
import plugins.globals.datacontainers.GlobalDataView;
import plugins.groups.support.GroupWeightingFunction;
import plugins.personproperties.datacontainers.PersonPropertyDataView;

import java.util.EnumMap;
import java.util.HashMap;
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
        Provides the weighting function used for biased selection of contacts from a given group type for
        transmission events
     */
    @Value.Derived
    public Map<ContactGroupType, Map<AgeGroup, GroupWeightingFunction>> groupWeightingFunctions() {
        Map<ContactGroupType, Map<AgeGroup, GroupWeightingFunction>> groupWeightingFunctionsMap =
                new EnumMap<>(ContactGroupType.class);

        for (Map.Entry<ContactGroupType, Map<AgeGroup, Map<AgeGroup, Double>>> entryForContactGroup :
                groupBiWeightingFunctionsMap().entrySet()) {
            Map<AgeGroup, GroupWeightingFunction> groupWeightingFunctionMap = groupWeightingFunctionsMap
                    .computeIfAbsent(entryForContactGroup.getKey(), x -> new HashMap<>());
            for (Map.Entry<AgeGroup, Map<AgeGroup, Double>> entryForAgeGroup : entryForContactGroup.getValue().entrySet()) {
                groupWeightingFunctionMap.put(entryForAgeGroup.getKey(),
                        (context, personId, groupId) -> {
                            GlobalDataView globalDataView = context.getDataView(GlobalDataView.class).get();
                            PersonPropertyDataView personPropertyDataView = context.getDataView(PersonPropertyDataView.class).get();
                            PopulationDescription populationDescription = globalDataView.getGlobalPropertyValue(
                                    GlobalProperty.POPULATION_DESCRIPTION);
                            AgeGroupPartition ageGroupPartition = populationDescription.ageGroupPartition();
                            AgeGroup ageGroup = ageGroupPartition.getAgeGroupFromIndex(
                                    personPropertyDataView.getPersonPropertyValue(personId, PersonProperty.AGE_GROUP_INDEX));
                            return entryForAgeGroup.getValue().getOrDefault(ageGroup, 0.0);
                        });
            }
        }

        return groupWeightingFunctionsMap;
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

