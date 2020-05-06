package gcm.core.epi.plugin.infection;

public enum InfectionPluginType {

    EXPONENTIAL(ExponentialPeriodInfectionPlugin.class),

    GAMMA(GammaPeriodInfectionPlugin.class);

    private final Class<? extends InfectionPlugin> pluginClass;

    InfectionPluginType(Class<? extends InfectionPlugin> pluginClass) {
        this.pluginClass = pluginClass;
    }

    public Class<? extends InfectionPlugin> getPluginClass() {
        return pluginClass;
    }

}
