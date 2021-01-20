package gcm.core.epi.plugin.transmission;

public enum TransmissionPluginType {

    SEASONAL(SeasonalTransmissionPlugin.class),

    VARIANT(VariantTransmissionPlugin.class);

    private final Class<? extends TransmissionPlugin> pluginClass;

    TransmissionPluginType(Class<? extends TransmissionPlugin> pluginClass) {
        this.pluginClass = pluginClass;
    }

    public Class<? extends TransmissionPlugin> getTransmissionPluginClass() {
        return pluginClass;
    }

}
