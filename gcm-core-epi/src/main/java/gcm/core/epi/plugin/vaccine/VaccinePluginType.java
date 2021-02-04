package gcm.core.epi.plugin.vaccine;

import gcm.core.epi.plugin.vaccine.combination.CombinationVaccinePlugin;
import gcm.core.epi.plugin.vaccine.onedose.OneDoseVaccinePlugin;
import gcm.core.epi.plugin.vaccine.resourcebased.DetailedResourceBasedVaccinePlugin;
import gcm.core.epi.plugin.vaccine.resourcebased.ResourceBasedVaccinePlugin;
import gcm.core.epi.plugin.vaccine.twodose.TwoDoseVaccinePlugin;

public enum VaccinePluginType {

    ONE_DOSE(OneDoseVaccinePlugin.class),

    TWO_DOSE(TwoDoseVaccinePlugin.class),

    COMBINATION(CombinationVaccinePlugin.class),

    RESOURCE_BASED(ResourceBasedVaccinePlugin.class),

    DETAILED_RESOURCE_BASED(DetailedResourceBasedVaccinePlugin.class);

    private final Class<? extends VaccinePlugin> vaccinePluginClass;

    VaccinePluginType(Class<? extends VaccinePlugin> vaccinePluginClass) {
        this.vaccinePluginClass = vaccinePluginClass;
    }

    public Class<? extends VaccinePlugin> getPluginClass() {
        return vaccinePluginClass;
    }

}
