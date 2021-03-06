package gcm.core.epi.components;

import gcm.components.AbstractComponent;
import gcm.core.epi.identifiers.*;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.HospitalData;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.util.distributions.GammaHelper;
import gcm.scenario.PersonId;
import gcm.scenario.PersonPropertyId;
import gcm.scenario.RegionId;
import gcm.simulation.Environment;
import gcm.simulation.Plan;
import gcm.util.geolocator.GeoLocator;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.Map;

public class HospitalizationManager extends AbstractComponent {

    @Override
    public void init(Environment environment) {

        // Register to observe people who may need to be hospitalized
        environment.observeGlobalPersonPropertyChange(true, PersonProperty.IS_SYMPTOMATIC);

    }

    @Override
    public void executePlan(Environment environment, Plan plan) {
        if (plan.getClass() == HospitalizationPlan.class) {
            HospitalizationPlan hospitalizationPlan = (HospitalizationPlan) plan;
            PersonId personId = hospitalizationPlan.personId;
            RegionId regionId = environment.getPersonRegion(personId);
            double lat = environment.getRegionPropertyValue(regionId, RegionProperty.LAT);
            double lon = environment.getRegionPropertyValue(regionId, RegionProperty.LON);

            // Is the person already hospitalized?
            //TODO: Update when ICU beds added
            if (environment.getPersonResourceLevel(personId, Resource.HOSPITAL_BED) != 0) {
                // Record when they were supposed to be discharged and remove the old plan
                double oldDischargeTime = environment.getPlanTime(personId);
                environment.removePlan(personId);

                System.out.println("Warning: Person attempted to be hospitalized due to reinfection who was already hospitalized");

                // Add new plan
                double additionalHospitalDuration = getHospitalizationDuration(environment, personId);
                environment.addPlan(new DischargePlan(personId), oldDischargeTime + additionalHospitalDuration,
                        personId);

                // Don't bother with the rest of the hospitalization logic
                return;
            }

            GeoLocator<HospitalData> hospitalDataGeoLocator = environment.getGlobalPropertyValue(
                    GlobalProperty.HOSPITAL_GEOLOCATOR);
            double hospitalizationMaxRadiusKM = environment.getGlobalPropertyValue(
                    GlobalProperty.HOSPITALIZATION_MAX_RADIUS_KM);

            List<Pair<HospitalData, Double>> prioritizedHospitals = hospitalDataGeoLocator.getPrioritizedLocations(lat, lon,
                    hospitalizationMaxRadiusKM);

            // TODO - process ICU and ventilator need here
            boolean receivedBed = false;
            for (Pair<HospitalData, Double> hospitalDataWithDistance : prioritizedHospitals) {
                RegionId hospitalRegionId = hospitalDataWithDistance.getFirst().regionId();
                if (environment.getRegionResourceLevel(hospitalRegionId, Resource.HOSPITAL_BED) > 0) {
                    if (!hospitalRegionId.equals(regionId)) {
                        environment.setPersonRegion(personId, hospitalRegionId);
                    }
                    environment.transferResourceToPerson(Resource.HOSPITAL_BED, personId, 1);
                    receivedBed = true;
                    break;
                }
            }
            if (!receivedBed) {
                environment.setPersonPropertyValue(personId, PersonProperty.DID_NOT_RECEIVE_HOSPITAL_BED, true);
            } else {
                // Make a plan to have a person be discharged
                double hospitalizationDuration = getHospitalizationDuration(environment, personId);

                environment.addPlan(new DischargePlan(personId),
                        environment.getTime() + hospitalizationDuration, personId);
            }
        } else if (plan.getClass() == DischargePlan.class) {
            PersonId personId = ((DischargePlan) plan).personId;
            environment.transferResourceFromPerson(Resource.HOSPITAL_BED, personId, 1);
        } else if (plan.getClass() == DeathPlan.class) {
            PersonId personId = ((DeathPlan) plan).personId;
            environment.setPersonPropertyValue(personId, PersonProperty.IS_DEAD, true);
            // Remove discharge plan and return hospital bed
            if (environment.getPlan(personId).isPresent()) {
                environment.removePlan(personId);
                environment.transferResourceFromPerson(Resource.HOSPITAL_BED, personId, 1);
            }

        } else {
            throw new RuntimeException("Unknown plan type " + plan.getClass() + " in Hospitalization Manager");
        }

    }

    @Override
    public void observePersonPropertyChange(Environment environment, PersonId personId, PersonPropertyId personPropertyId) {

        if (personPropertyId == PersonProperty.IS_SYMPTOMATIC) {
            boolean isSymptomatic = environment.getPersonPropertyValue(personId, PersonProperty.IS_SYMPTOMATIC);
            if (isSymptomatic) {
                PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                        GlobalProperty.POPULATION_DESCRIPTION);
                Integer ageGroupIndex = environment.getPersonPropertyValue(personId, PersonProperty.AGE_GROUP_INDEX);
                AgeGroup ageGroup = populationDescription.ageGroupPartition().getAgeGroupFromIndex(ageGroupIndex);
                // Determine if person will be a hospitalization severity case
                Map<AgeGroup, Double> caseHospitalizationRatios = environment.getGlobalPropertyValue(
                        GlobalProperty.CASE_HOSPITALIZATION_RATIO);
                Double caseHospitalizationRatio = caseHospitalizationRatios.getOrDefault(ageGroup, 0.0);

                if (environment.getRandomGeneratorFromId(RandomId.HOSPITALIZATION_MANAGER).nextDouble() <= caseHospitalizationRatio) {

                    // Declare that they had severe enough illness to warrant hospitalization
                    environment.setPersonPropertyValue(personId, PersonProperty.EVER_HAD_SEVERE_ILLNESS, true);

                    // Make a plan to have a person seek hospitalization
                    Map<AgeGroup, Double> hospitalizationDelayMeans = environment.getGlobalPropertyValue(
                            GlobalProperty.HOSPITALIZATION_DELAY_MEAN);
                    Map<AgeGroup, Double> hospitalizationDelaySDs = environment.getGlobalPropertyValue(
                            GlobalProperty.HOSPITALIZATION_DELAY_SD);

                    // Age-group specific values
                    Double hospitalizationDelayMean = hospitalizationDelayMeans.get(ageGroup);
                    Double hospitalizationDelaySD = hospitalizationDelaySDs.get(ageGroup);

                    RealDistribution hospitalDelayDistribution = new GammaDistribution(
                            environment.getRandomGeneratorFromId(RandomId.HOSPITALIZATION_MANAGER),
                            GammaHelper.getShapeFromMeanAndSD(hospitalizationDelayMean, hospitalizationDelaySD),
                            GammaHelper.getScaleFromMeanAndSD(hospitalizationDelayMean, hospitalizationDelaySD));

                    double hospitalizationTime = environment.getTime() + hospitalDelayDistribution.sample();
                    environment.addPlan(new HospitalizationPlan(personId), hospitalizationTime);

                    // Handle possibility of death
                    Map<AgeGroup, Double> caseFatalityRatios = environment.getGlobalPropertyValue(
                            GlobalProperty.CASE_FATALITY_RATIO);

                    // Age-group specific values
                    double hospitalizationFatalityRatio = caseFatalityRatios.get(ageGroup) /
                            caseHospitalizationRatios.get(ageGroup);

                    if (environment.getRandomGeneratorFromId(RandomId.HOSPITALIZATION_MANAGER).nextDouble() < hospitalizationFatalityRatio) {
                        // Make a plan for a person to die
                        Map<AgeGroup, Double> hospitalizationDeathDelayMeans = environment.getGlobalPropertyValue(
                                GlobalProperty.HOSPITALIZATION_TO_DEATH_DELAY_MEAN);
                        Map<AgeGroup, Double> hospitalizationDeathDelaySDs = environment.getGlobalPropertyValue(
                                GlobalProperty.HOSPITALIZATION_TO_DEATH_DELAY_SD);

                        double hospitalizationDeathDelayMean = hospitalizationDeathDelayMeans.get(ageGroup);
                        double hospitalizationDeathDelaySD = hospitalizationDeathDelaySDs.get(ageGroup);

                        RealDistribution hospitalizationDeathDelayDistribution = new GammaDistribution(
                                environment.getRandomGeneratorFromId(RandomId.HOSPITALIZATION_MANAGER),
                                GammaHelper.getShapeFromMeanAndSD(hospitalizationDeathDelayMean, hospitalizationDeathDelaySD),
                                GammaHelper.getScaleFromMeanAndSD(hospitalizationDeathDelayMean, hospitalizationDeathDelaySD));

                        environment.addPlan(new DeathPlan(personId),
                                hospitalizationTime + hospitalizationDeathDelayDistribution.sample());

                    }
                }
            }
        }
    }

    private double getHospitalizationDuration(Environment environment, PersonId personId) {
        PopulationDescription populationDescription = environment.getGlobalPropertyValue(
                GlobalProperty.POPULATION_DESCRIPTION);
        Integer ageGroupIndex = environment.getPersonPropertyValue(personId, PersonProperty.AGE_GROUP_INDEX);
        AgeGroup ageGroup = populationDescription.ageGroupPartition().getAgeGroupFromIndex(ageGroupIndex);

        Map<AgeGroup, Double> hospitalizationDurationMeans = environment.getGlobalPropertyValue(GlobalProperty.HOSPITALIZATION_DURATION_MEAN);
        Map<AgeGroup, Double> hospitalizationDurationSDs = environment.getGlobalPropertyValue(GlobalProperty.HOSPITALIZATION_DURATION_SD);

        // Age-group specific values
        Double hospitalizationDurationMean = hospitalizationDurationMeans.get(ageGroup);
        Double hospitalizationDurationSD = hospitalizationDurationSDs.get(ageGroup);

        RealDistribution hospitalizationDurationDistribution = new GammaDistribution(
                environment.getRandomGeneratorFromId(RandomId.HOSPITALIZATION_MANAGER),
                GammaHelper.getShapeFromMeanAndSD(hospitalizationDurationMean, hospitalizationDurationSD),
                GammaHelper.getScaleFromMeanAndSD(hospitalizationDurationMean, hospitalizationDurationSD));

        return hospitalizationDurationDistribution.sample();

    }

    private static class HospitalizationPlan implements Plan {

        final PersonId personId;

        HospitalizationPlan(PersonId personId) {
            this.personId = personId;
        }

    }

    private static class DischargePlan implements Plan {

        final PersonId personId;

        DischargePlan(PersonId personId) {
            this.personId = personId;
        }

    }

    private static class DeathPlan implements Plan {

        final PersonId personId;

        DeathPlan(PersonId personId) {
            this.personId = personId;
        }

    }

}
