package gcm.core.epi.util.logging;

import gcm.core.epi.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plugins.components.support.OutputItem;
import plugins.gcm.experiment.ReplicationId;
import plugins.gcm.experiment.ScenarioId;
import plugins.gcm.experiment.output.LogItem;
import plugins.gcm.experiment.output.LogStatus;
import plugins.gcm.experiment.output.OutputItemHandler;
import plugins.gcm.experiment.progress.ExperimentProgressLog;

import java.util.LinkedHashSet;
import java.util.Set;

public class LogItemHandler implements OutputItemHandler {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    @Override
    public void openSimulation(ScenarioId scenarioId, ReplicationId replicationId) {
        // Do nothing
    }

    @Override
    public void openExperiment(ExperimentProgressLog experimentProgressLog) {
        // Do nothing
    }

    @Override
    public void closeSimulation(ScenarioId scenarioId, ReplicationId replicationId) {
        // Do nothing
    }

    @Override
    public void closeExperiment() {
        // Do nothing
    }

    @Override
    public void handle(ScenarioId scenarioId, ReplicationId replicationId, OutputItem outputItem) {
        LogItem logItem = (LogItem) outputItem;
        StringBuilder sb = new StringBuilder();
        if (scenarioId.getValue() != -1 || replicationId.getValue() != -1) {
            sb.append("[Scenario = ");
            sb.append(scenarioId.getValue());
            sb.append(", Replication = ");
            sb.append(replicationId.getValue());
            sb.append("] - ");
        }
        sb.append(logItem.getMessage());
        String message = sb.toString();

        if (logItem.getLogStatus() == LogStatus.ERROR) {
            logger.error(message);
        } else {
            logger.info(message);
        }
    }

    @Override
    public Set<Class<? extends OutputItem>> getHandledClasses() {
        Set<Class<? extends OutputItem>> result = new LinkedHashSet<>();
        result.add(LogItem.class);
        return result;
    }

}
