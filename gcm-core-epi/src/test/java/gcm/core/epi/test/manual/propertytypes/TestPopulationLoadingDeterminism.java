package gcm.core.epi.test.manual.propertytypes;

import gcm.core.epi.population.AgeGroupPartition;
import gcm.core.epi.population.PopulationDescription;
import gcm.core.epi.util.loading.CoreEpiBootstrapUtil;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;

import java.io.IOException;
import java.nio.file.Path;

public class TestPopulationLoadingDeterminism {

    private static final String AGE_GROUP_FILE = "~/Desktop/Coreflu/input/transmission/default-age-groups-5-classes.yaml";
    private static final String POPULATION_FILE = "~/Desktop/Coreflu/input/population/all_states/pa.csv";

    @Test
    public void test() throws IOException {

        final Path ageGroupFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(AGE_GROUP_FILE);
        AgeGroupPartition ageGroupPartition = CoreEpiBootstrapUtil.loadAgeGroupsFromFile(ageGroupFilePath);

        final Path populationFilePath = CoreEpiBootstrapUtil.getPathFromRelativeString(POPULATION_FILE);

        CoreEpiBootstrapUtil coreEpiBootstrapUtil = new CoreEpiBootstrapUtil();
        long startTime = java.lang.System.currentTimeMillis();
        PopulationDescription populationDescription = coreEpiBootstrapUtil.loadPopulationDescriptionFromFile(populationFilePath, ageGroupPartition);
        long endTime = java.lang.System.currentTimeMillis();

        System.out.println(populationDescription.toString() + " loaded in " + (endTime-startTime)/1000.0 + "s" + " " + populationDescription.hashCode());

        //System.out.println(GraphLayout.parseInstance(populationDescription).toFootprint());

    }

}
