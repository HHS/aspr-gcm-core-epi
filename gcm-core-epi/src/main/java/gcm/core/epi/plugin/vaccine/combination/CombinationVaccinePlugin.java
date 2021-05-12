package gcm.core.epi.plugin.vaccine.combination;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.plugin.vaccine.VaccinePlugin;
import gcm.core.epi.plugin.vaccine.onedose.OneDoseVaccineEfficacySpecification;
import gcm.core.epi.plugin.vaccine.onedose.OneDoseVaccineHelper;
import gcm.core.epi.plugin.vaccine.onedose.OneDoseVaccineStatus;
import gcm.core.epi.plugin.vaccine.twodose.TwoDoseVaccineEfficacySpecification;
import gcm.core.epi.plugin.vaccine.twodose.TwoDoseVaccineHelper;
import gcm.core.epi.plugin.vaccine.twodose.TwoDoseVaccineStatus;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.variants.VariantId;
import plugins.gcm.agents.Plan;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import plugins.gcm.agents.AbstractComponent;
import plugins.gcm.agents.Environment;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.globals.support.GlobalPropertyId;
import plugins.partitions.support.Equality;
import plugins.partitions.support.Partition;
import plugins.partitions.support.PartitionSampler;
import plugins.people.support.PersonId;
import plugins.personproperties.support.PersonPropertyId;
import plugins.personproperties.support.PersonPropertyLabeler;
import plugins.personproperties.support.PropertyFilter;
import plugins.regions.support.RegionPropertyId;
import plugins.stochastics.support.RandomNumberGeneratorId;
import util.MultiKey;

import java.util.*;

/*
    Combines two independent vaccines - one single dose and one two-dose
 */
public class CombinationVaccinePlugin implements VaccinePlugin {

    private final List<VaccineType> vaccineTypes;

    public CombinationVaccinePlugin(List<VaccineType> vaccineTypes) {
        this.vaccineTypes = vaccineTypes;
    }

    /*
        Returns the actual ID used in the simulation for the specific property (global, region, person)
            Typical behavior will be to prepend the vaccine id in a multi-key object
            This enables the re-use of the single and two dose vaccine plugin logic
     */
    public static GeneralIndexedPropertyId getPropertyIdForVaccine(int vaccineId, Object propertyId) {
        return new GeneralIndexedPropertyId(vaccineId, propertyId);
    }

    @Override
    public List<RandomNumberGeneratorId> getRandomIds() {
        List<RandomNumberGeneratorId> randomIds = new ArrayList<>();
        randomIds.add(VaccineRandomId.ID);
        return randomIds;
    }

    @Override
    public double getVES(Environment environment, PersonId personId, VariantId variantId) {
        int i = 0;
        double vaccineFailureProbability = 1.0;
        for (VaccineType vaccineType : vaccineTypes) {
            switch (vaccineType) {
                case ONE_DOSE:
                    vaccineFailureProbability *=
                            1.0 - OneDoseVaccineHelper.getVES(environment, personId,
                                    getPropertyIdForVaccine(i, VaccinePersonProperty.VACCINE_STATUS),
                                    environment.getGlobalPropertyValue(
                                            getPropertyIdForVaccine(i, VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION)));
                    break;
                case TWO_DOSE:
                    vaccineFailureProbability *=
                            1.0 - TwoDoseVaccineHelper.getVES(environment, personId,
                                    getPropertyIdForVaccine(i, VaccinePersonProperty.VACCINE_STATUS),
                                    environment.getGlobalPropertyValue(
                                            getPropertyIdForVaccine(i, VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION)));
                    break;
            }
            i++;
        }
        return 1 - vaccineFailureProbability;
    }

    @Override
    public double getVEI(Environment environment, PersonId personId, VariantId variantId) {
        int i = 0;
        double vaccineFailureProbability = 1.0;
        for (VaccineType vaccineType : vaccineTypes) {
            switch (vaccineType) {
                case ONE_DOSE:
                    vaccineFailureProbability *=
                            1.0 - OneDoseVaccineHelper.getVEI(environment, personId,
                                    getPropertyIdForVaccine(i, VaccinePersonProperty.VACCINE_STATUS),
                                    environment.getGlobalPropertyValue(
                                            getPropertyIdForVaccine(i, VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION)));
                    break;
                case TWO_DOSE:
                    vaccineFailureProbability *=
                            1.0 - TwoDoseVaccineHelper.getVEI(environment, personId,
                                    getPropertyIdForVaccine(i, VaccinePersonProperty.VACCINE_STATUS),
                                    environment.getGlobalPropertyValue(
                                            getPropertyIdForVaccine(i, VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION)));
                    break;
            }
            i++;
        }
        return 1 - vaccineFailureProbability;
    }

    @Override
    public double getVEP(Environment environment, PersonId personId, VariantId variantId) {
        int i = 0;
        double vaccineFailureProbability = 1.0;
        for (VaccineType vaccineType : vaccineTypes) {
            switch (vaccineType) {
                case ONE_DOSE:
                    vaccineFailureProbability *=
                            1.0 - OneDoseVaccineHelper.getVEP(environment, personId,
                                    getPropertyIdForVaccine(i, VaccinePersonProperty.VACCINE_STATUS),
                                    environment.getGlobalPropertyValue(
                                            getPropertyIdForVaccine(i, VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION)));
                    break;
                case TWO_DOSE:
                    vaccineFailureProbability *=
                            1.0 - TwoDoseVaccineHelper.getVEP(environment, personId,
                                    getPropertyIdForVaccine(i, VaccinePersonProperty.VACCINE_STATUS),
                                    environment.getGlobalPropertyValue(
                                            getPropertyIdForVaccine(i, VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION)));
                    break;
            }
            i++;
        }
        return 1 - vaccineFailureProbability;
    }

    @Override
    public double getVED(Environment environment, PersonId personId, VariantId variantId) {
        return 0.0;
    }

    @Override
    public void load(ExperimentBuilder experimentBuilder) {
        VaccinePlugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalComponentId(VACCINE_MANAGER_IDENTIFIER, () -> new VaccineManager()::init);
    }

    public enum VaccineType {

        ONE_DOSE,

        TWO_DOSE

    }

    public enum VaccinePersonProperty implements PersonPropertyId {

        VACCINE_STATUS

    }

    public enum VaccineGlobalProperty implements GlobalPropertyId {

        VACCINE_EFFICACY_SPECIFICATION,

        VACCINATION_START_DAY,

        VACCINATION_RATE_PER_DAY,

        VACCINE_UPTAKE_WEIGHTS

    }

    /*
        Helper class to wrap a global, region, or person property id with an index.
        TODO: Make this more strongly typed or refactor to eliminate
     */
    static class GeneralIndexedPropertyId implements GlobalPropertyId, RegionPropertyId, PersonPropertyId {
        final int vaccineId;
        final Object propertyId;

        GeneralIndexedPropertyId(int vaccineId, Object propertyId) {
            this.vaccineId = vaccineId;
            this.propertyId = propertyId;
        }

        @Override
        public String toString() {
            return "GeneralIndexedPropertyId{" +
                    "vaccineId=" + vaccineId +
                    ", propertyId=" + propertyId +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GeneralIndexedPropertyId that = (GeneralIndexedPropertyId) o;
            return vaccineId == that.vaccineId &&
                    Objects.equals(propertyId, that.propertyId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vaccineId, propertyId);
        }
    }

    class VaccineManager extends AbstractComponent {

        // Keys for vaccine indexes
        private final Object TO_VACCINATE_PARTITION_KEY = new Object();
        private final Map<Integer, Object> perVaccinePartitionKeys = new HashMap<>();
        private final Map<Integer, RealDistribution> perVaccineInterVaccinationDelayDistribution = new HashMap<>();

        @Override
        public void init(Environment environment) {

            PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                    GlobalProperty.POPULATION_DESCRIPTION);
            List<AgeGroup> ageGroups = populationDescription.ageGroupPartition().ageGroupList();

            int vaccineId = 0;
            for (VaccineType vaccineType : vaccineTypes) {
                final double vaccinationRatePerDay = environment.getGlobalPropertyValue(getPropertyIdForVaccine(vaccineId,
                        VaccineGlobalProperty.VACCINATION_RATE_PER_DAY));
                if (vaccinationRatePerDay > 0) {
                    // Make distribution for inter-vaccination time delays (reserved 1/2 for first and second doses when needed)
                    final RandomGenerator randomGenerator = environment.getRandomGeneratorFromId(VaccineRandomId.ID);
                    RealDistribution interVaccinationDelayDistribution = new ExponentialDistribution(randomGenerator,
                            (vaccineType == VaccineType.ONE_DOSE ? 1.0 : 2.0) / vaccinationRatePerDay);
                    perVaccineInterVaccinationDelayDistribution.put(vaccineId, interVaccinationDelayDistribution);

                    // Random vaccination target indexes
                    Object partitionKey = new MultiKey(vaccineId, TO_VACCINATE_PARTITION_KEY);
                    Object notVaccinatedStatus;
                    switch (vaccineTypes.get(vaccineId)) {
                        case ONE_DOSE:
                            notVaccinatedStatus = OneDoseVaccineStatus.NOT_VACCINATED;
                            break;
                        case TWO_DOSE:
                            notVaccinatedStatus = TwoDoseVaccineStatus.NOT_VACCINATED;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected vaccine type: " + vaccineTypes.get(vaccineId));
                    }
                    environment.addPartition(Partition.builder()
                                    // Filter by vaccine status
                                    .setFilter(new PropertyFilter(getPropertyIdForVaccine(vaccineId,
                                            VaccinePersonProperty.VACCINE_STATUS), Equality.EQUAL, notVaccinatedStatus))
                                    // Partition by age group
                                    .addLabeler(new PersonPropertyLabeler(PersonProperty.AGE_GROUP_INDEX,
                                            ageGroupIndex -> ageGroups.get((int) ageGroupIndex)))
                                    .build(),
                            partitionKey);
                    perVaccinePartitionKeys.put(vaccineId, partitionKey);

                    // Schedule first vaccination event
                    final double vaccinationStartDay = environment.getGlobalPropertyValue(
                            getPropertyIdForVaccine(vaccineId, VaccineGlobalProperty.VACCINATION_START_DAY));
                    environment.addPlan(new FirstDoseVaccinationPlan(vaccineId), vaccinationStartDay);
                }
                vaccineId++;
            }

        }

        @Override
        public void executePlan(Environment environment, Plan plan) {
            final Class<?> planClass = plan.getClass();
            if (planClass == FirstDoseVaccinationPlan.class) {
                // Handle first (and potentially only) dose
                final int vaccineId = ((FirstDoseVaccinationPlan) plan).vaccineId;
                handleFirstDoseAndScheduleNext(environment, vaccineId);

            } else if (planClass == SecondDoseVaccinationPlan.class) {
                // Handle second dose
                SecondDoseVaccinationPlan secondDoseVaccinationPlan = (SecondDoseVaccinationPlan) plan;
                handleSecondDoseAndScheduleNext(environment, secondDoseVaccinationPlan.vaccineId,
                        secondDoseVaccinationPlan.personId);

            } else if (planClass == VaccineProtectionTogglePlan.class) {
                // Handle toggling vaccine protection
                VaccineProtectionTogglePlan vaccineProtectionTogglePlan = (VaccineProtectionTogglePlan) plan;
                final int vaccineId = ((VaccineProtectionTogglePlan) plan).vaccineId;
                VaccineType vaccineType = vaccineTypes.get(vaccineId);
                switch (vaccineType) {
                    case ONE_DOSE:
                        handleVaccineProtectionToggleOneDose(environment, vaccineId, vaccineProtectionTogglePlan.personId);
                        break;
                    case TWO_DOSE:
                        handleVaccineProtectionToggleTwoDose(environment, vaccineId, vaccineProtectionTogglePlan.personId);
                        break;
                }
            }
        }

        private void handleFirstDoseAndScheduleNext(Environment environment, int vaccineId) {
            // First select a random person to vaccinate, if possible, taking into account vaccine uptake weights
            AgeWeights vaccineUptakeWeights = environment.getGlobalPropertyValue(
                    getPropertyIdForVaccine(vaccineId, VaccineGlobalProperty.VACCINE_UPTAKE_WEIGHTS));
            Object vaccinePartitionKey = perVaccinePartitionKeys.get(vaccineId);


            final Optional<PersonId> personId = environment.samplePartition(vaccinePartitionKey, PartitionSampler.builder()
                    .setLabelSetWeightingFunction((context, labelSet) -> {
                        // We know this labelSet will have a label for this person property
                        //noinspection OptionalGetWithoutIsPresent
                        AgeGroup ageGroup = (AgeGroup) labelSet.getLabel(PersonProperty.AGE_GROUP_INDEX).get();
                        return vaccineUptakeWeights.getWeight(ageGroup);
                    })
                    .setRandomNumberGeneratorId(VaccineRandomId.ID)
                    .build());

            if (personId.isPresent()) {
                // As someone is left to vaccinate, now actually perform this
                switch (vaccineTypes.get(vaccineId)) {
                    case ONE_DOSE:
                        performFirstDoseVaccinationForOneDose(environment, vaccineId, personId.get());
                        break;
                    case TWO_DOSE:
                        performFirstDoseVaccinationForTwoDose(environment, vaccineId, personId.get());
                        break;
                }

                // Schedule next vaccination for this vaccine
                final RealDistribution interVaccinationDelayDistribution =
                        perVaccineInterVaccinationDelayDistribution.get(vaccineId);
                final double vaccinationTime = environment.getTime() + interVaccinationDelayDistribution.sample();
                environment.addPlan(new FirstDoseVaccinationPlan(vaccineId), vaccinationTime);
            }
            // Nobody is left to vaccinate, so stop
            // TODO: Could modify this to allow for people to be added to the simulation or other similar changes
        }

        private void performFirstDoseVaccinationForOneDose(Environment environment, int vaccineId, PersonId personId) {
            OneDoseVaccineEfficacySpecification efficacySpecification =
                    environment.getGlobalPropertyValue(getPropertyIdForVaccine(vaccineId,
                            VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION));
            final PersonPropertyId vaccineStatusId = getPropertyIdForVaccine(vaccineId, VaccinePersonProperty.VACCINE_STATUS);

            double vaccineEfficacyDuration = efficacySpecification.efficacyDurationDays();
            if (vaccineEfficacyDuration > 0) {
                double vaccineEfficacyDelay = efficacySpecification.efficacyDelayDays();
                if (vaccineEfficacyDelay > 0) {
                    environment.setPersonPropertyValue(personId, vaccineStatusId,
                            OneDoseVaccineStatus.VACCINATED_NOT_YET_PROTECTED);
                    environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                            environment.getTime() + vaccineEfficacyDelay);
                } else {
                    environment.setPersonPropertyValue(personId, vaccineStatusId,
                            OneDoseVaccineStatus.VACCINE_PROTECTED);
                    // Schedule end of protection
                    if (vaccineEfficacyDuration < Double.POSITIVE_INFINITY) {
                        environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                                environment.getTime() + vaccineEfficacyDuration);
                    }
                }
            }
        }

        private void performFirstDoseVaccinationForTwoDose(Environment environment, int vaccineId, PersonId personId) {
            TwoDoseVaccineEfficacySpecification efficacySpecification =
                    environment.getGlobalPropertyValue(getPropertyIdForVaccine(vaccineId,
                            VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION));

            double vaccineEfficacyDuration = efficacySpecification.efficacyDurationDays();
            if (vaccineEfficacyDuration > 0) {
                double vaccineEfficacyDelay = efficacySpecification.efficacyDelayDays();
                double interDoseDelay = efficacySpecification.interDoseDelayDays();
                final PersonPropertyId vaccineStatusId = getPropertyIdForVaccine(vaccineId, VaccinePersonProperty.VACCINE_STATUS);
                if (vaccineEfficacyDelay > 0) {
                    if (interDoseDelay > 0) {
                        // Administer only the first dose with delayed protection
                        environment.setPersonPropertyValue(personId, vaccineStatusId,
                                TwoDoseVaccineStatus.VACCINATED_ONE_DOSE_NOT_YET_PROTECTED);

                    } else {
                        // Administer both doses simultaneously with delayed protection
                        environment.setPersonPropertyValue(personId, vaccineStatusId,
                                TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_NOT_YET_PROTECTED);

                    }
                    environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                            environment.getTime() + vaccineEfficacyDelay);
                } else {
                    if (interDoseDelay > 0) {
                        // Administer the first dose with immediate protection
                        environment.setPersonPropertyValue(personId, vaccineStatusId,
                                TwoDoseVaccineStatus.VACCINATED_ONE_DOSE_PROTECTED);

                    } else {
                        // Administer both doses simultaneously with immediate protection
                        environment.setPersonPropertyValue(personId, vaccineStatusId,
                                TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PROTECTED);
                        // Schedule end of protection
                        if (vaccineEfficacyDuration < Double.POSITIVE_INFINITY) {
                            environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                                    environment.getTime() + vaccineEfficacyDuration);
                        }
                    }
                }
            }
        }

        private void handleSecondDoseAndScheduleNext(Environment environment, int vaccineId, PersonId personId) {
            TwoDoseVaccineEfficacySpecification efficacySpecification =
                    environment.getGlobalPropertyValue(getPropertyIdForVaccine(vaccineId,
                            VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION));
            final double vaccineEfficacyDelay = efficacySpecification.efficacyDelayDays();

            final PersonPropertyId vaccineStatusId = getPropertyIdForVaccine(vaccineId, VaccinePersonProperty.VACCINE_STATUS);
            final TwoDoseVaccineStatus vaccineStatus = environment.getPersonPropertyValue(personId,
                    getPropertyIdForVaccine(vaccineId, VaccinePersonProperty.VACCINE_STATUS));

            if (vaccineStatus == TwoDoseVaccineStatus.VACCINATED_ONE_DOSE_NOT_YET_PROTECTED) {
                if (vaccineEfficacyDelay > 0) {
                    // Administer second dose but schedule protection for later
                    environment.setPersonPropertyValue(personId, vaccineStatusId,
                            TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_NOT_YET_PROTECTED);
                    environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                            environment.getTime() + vaccineEfficacyDelay);
                } else {
                    // This should not happen with the existing logic
                    throw new RuntimeException("Vaccine Manager had inconsistent vaccine effectiveness delay logic");
                }
            } else if (vaccineStatus == TwoDoseVaccineStatus.VACCINATED_ONE_DOSE_PROTECTED) {
                if (vaccineEfficacyDelay > 0) {
                    // Administer second dose but schedule protection for later
                    environment.setPersonPropertyValue(personId, vaccineStatusId,
                            TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PARTIALLY_PROTECTED);
                    environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                            environment.getTime() + vaccineEfficacyDelay);
                } else {
                    environment.setPersonPropertyValue(personId, vaccineStatusId,
                            TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PROTECTED);
                    final double vaccineEfficacyDuration = efficacySpecification.efficacyDurationDays();
                    if (vaccineEfficacyDuration < Double.POSITIVE_INFINITY) {
                        environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                                environment.getTime() + vaccineEfficacyDuration);
                    }
                }

            }

        }

        private void handleVaccineProtectionToggleOneDose(Environment environment, int vaccineId, PersonId personId) {
            OneDoseVaccineEfficacySpecification efficacySpecification =
                    environment.getGlobalPropertyValue(getPropertyIdForVaccine(vaccineId,
                            VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION));

            final PersonPropertyId vaccineStatusId = getPropertyIdForVaccine(vaccineId, VaccinePersonProperty.VACCINE_STATUS);
            final OneDoseVaccineStatus vaccineStatus = environment.getPersonPropertyValue(personId, vaccineStatusId);

            // If person is not yet vaccine protected, make them so
            if (vaccineStatus == OneDoseVaccineStatus.VACCINATED_NOT_YET_PROTECTED) {
                // Make person vaccine protected
                environment.setPersonPropertyValue(personId, vaccineStatusId,
                        OneDoseVaccineStatus.VACCINE_PROTECTED);

                // Handle possibility of finite VE duration, scheduling toggling of vaccine protection off
                double vaccineEfficacyDuration = efficacySpecification.efficacyDurationDays();
                if (vaccineEfficacyDuration < Double.POSITIVE_INFINITY) {
                    environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                            environment.getTime() + vaccineEfficacyDuration);
                }
            } else {  // Person is already vaccine protected, so toggle vaccine protection off
                environment.setPersonPropertyValue(personId, vaccineStatusId,
                        OneDoseVaccineStatus.VACCINATED_NO_LONGER_PROTECTED);
            }
        }

        private void handleVaccineProtectionToggleTwoDose(Environment environment, int vaccineId, PersonId personId) {
            TwoDoseVaccineEfficacySpecification efficacySpecification =
                    environment.getGlobalPropertyValue(getPropertyIdForVaccine(vaccineId,
                            VaccineGlobalProperty.VACCINE_EFFICACY_SPECIFICATION));

            final PersonPropertyId vaccineStatusId = getPropertyIdForVaccine(vaccineId, VaccinePersonProperty.VACCINE_STATUS);
            final TwoDoseVaccineStatus vaccineStatus = environment.getPersonPropertyValue(personId, vaccineStatusId);

            // Progress their protection to the next stage or else end it
            if (vaccineStatus == TwoDoseVaccineStatus.VACCINATED_ONE_DOSE_NOT_YET_PROTECTED) {
                // Make first dose protective
                environment.setPersonPropertyValue(personId, vaccineStatusId,
                        TwoDoseVaccineStatus.VACCINATED_ONE_DOSE_PROTECTED);
            } else if (vaccineStatus == TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_NOT_YET_PROTECTED) {
                final double interDoseDelay = efficacySpecification.interDoseDelayDays();
                if (interDoseDelay > 0) {
                    // Only protected by first dose
                    environment.setPersonPropertyValue(personId, vaccineStatusId,
                            TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PARTIALLY_PROTECTED);
                } else {
                    // Protected by both doses at same time as they were co-administered
                    environment.setPersonPropertyValue(personId, vaccineStatusId,
                            TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PROTECTED);
                }

            } else if (vaccineStatus == TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PARTIALLY_PROTECTED) {
                // Make person vaccine protected from both doses
                environment.setPersonPropertyValue(personId, vaccineStatusId,
                        TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PROTECTED);
                // Handle possibility of finite VE duration, scheduling toggling of vaccine protection off
                final double vaccineEfficacyDuration = efficacySpecification.efficacyDurationDays();
                if (vaccineEfficacyDuration < Double.POSITIVE_INFINITY) {
                    environment.addPlan(new VaccineProtectionTogglePlan(vaccineId, personId),
                            environment.getTime() + vaccineEfficacyDuration);
                }

            } else if (vaccineStatus == TwoDoseVaccineStatus.VACCINATED_TWO_DOSES_PROTECTED) {
                // Person is already vaccine protected, so toggle vaccine protection off
                environment.setPersonPropertyValue(personId, vaccineStatusId,
                        TwoDoseVaccineStatus.VACCINATED_NO_LONGER_PROTECTED);
            } else {
                throw new RuntimeException("VaccineManager has a VaccineProtectionTogglePlan for an invalid status");
            }

        }

        /*
            A plan to vaccinate a random person from the population with the first dose
         */
        private class FirstDoseVaccinationPlan implements Plan {
            final int vaccineId;

            private FirstDoseVaccinationPlan(int vaccineId) {
                this.vaccineId = vaccineId;
            }
        }

        /*
            A plan to vaccinate a specific person from the population with a second dose
         */
        private class SecondDoseVaccinationPlan implements Plan {
            final int vaccineId;
            final PersonId personId;

            private SecondDoseVaccinationPlan(int vaccineId, PersonId personId) {
                this.vaccineId = vaccineId;
                this.personId = personId;
            }
        }

        /*
            A plan to toggle vaccine protection on or off
         */
        private class VaccineProtectionTogglePlan implements Plan {
            final int vaccineId;
            final PersonId personId;

            private VaccineProtectionTogglePlan(int vaccineId, PersonId personId) {
                this.vaccineId = vaccineId;
                this.personId = personId;
            }
        }

    }

}
