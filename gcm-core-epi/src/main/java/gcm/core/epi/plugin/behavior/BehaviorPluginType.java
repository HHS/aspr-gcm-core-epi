package gcm.core.epi.plugin.behavior;

public enum BehaviorPluginType {

    ISOLATION_AND_HYGIENE(IsolationHygieneBehaviorPlugin.class),

    SCHOOL_CLOSURE(SchoolClosureBehaviorPlugin.class),

    WORKPLACE_TELEWORK(TeleworkBehaviorPlugin.class),

    LOCATION_INFECTION_REDUCTION(LocationInfectionReductionPlugin.class),

    CONTACT_TRACING(ContactTracingBehaviorPlugin.class),

    RANDOM_TESTING(RandomTestingBehaviorPlugin.class),

    COMBINATION_BEHAVIOR(CombinationBehaviorPlugin.class),

    TRIGGER_TEST(TriggerTestBehaviorPlugin.class);

    private final Class<? extends BehaviorPlugin> pluginClass;

    BehaviorPluginType(Class<? extends BehaviorPlugin> pluginClass) {
        this.pluginClass = pluginClass;
    }

    public Class<? extends BehaviorPlugin> getBehaviorPluginClass() {
        return pluginClass;
    }

}
