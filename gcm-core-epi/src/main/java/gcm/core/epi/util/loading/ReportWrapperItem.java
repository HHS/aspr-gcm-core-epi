package gcm.core.epi.util.loading;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.propertytypes.FipsScope;
import org.immutables.value.Value;
import plugins.reports.support.ReportPeriod;

import java.util.Set;

@Value.Immutable
@JsonDeserialize(as = ImmutableReportWrapperItem.class)
public abstract class ReportWrapperItem {
    public abstract String report();

    public abstract Set<String> items();

    @Value.Default
    public ReportPeriod period() {
        return ReportPeriod.DAILY;
    }

    @Value.Default
    public FipsScope regionAggregationLevel() {
        return FipsScope.TRACT;
    }

    @Value.Default
    public String file() {
        return report().toLowerCase() + ".tsv";
    }

    /**
     * Only used for PERSON_RESOURCE
     */
    @SuppressWarnings("SameReturnValue")
    @Value.Default
    public boolean reportPeopleWithoutResources() {
        return false;
    }

    /**
     * Only used for PERSON_RESOURCE
     */
    @SuppressWarnings("SameReturnValue")
    @Value.Default
    public boolean reportZeroPopulations() {
        return false;
    }

    /**
     * Only used for INFECTION_REPORT
     */
    @SuppressWarnings("SameReturnValue")
    @Value.Default
    public boolean showTransmissionAttempts() {
        return false;
    }
}
