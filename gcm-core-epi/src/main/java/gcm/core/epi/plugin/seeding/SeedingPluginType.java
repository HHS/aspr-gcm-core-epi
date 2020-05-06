package gcm.core.epi.plugin.seeding;

import gcm.core.epi.plugin.Plugin;

public enum SeedingPluginType {

    EXPONENTIAL(ExponentialSeedingPlugin.class);

    private final Class<? extends Plugin> pluginClass;

    SeedingPluginType(Class<? extends Plugin> pluginClass) {
        this.pluginClass = pluginClass;
    }

    public Class<? extends Plugin> getPluginClass() {
        return pluginClass;
    }

}
