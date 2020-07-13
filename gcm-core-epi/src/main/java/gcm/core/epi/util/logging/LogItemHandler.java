package gcm.core.epi.util.logging;

import gcm.core.epi.Runner;
import gcm.experiment.progress.ExperimentProgressLog;
import gcm.output.OutputItem;
import gcm.output.OutputItemHandler;
import gcm.output.simstate.LogItem;
import gcm.output.simstate.LogStatus;
import gcm.scenario.ReplicationId;
import gcm.scenario.ScenarioId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void handle(OutputItem outputItem) {
        LogItem logItem = (LogItem) outputItem;
        StringBuilder sb = new StringBuilder();
        if (logItem.getScenarioId().getValue() != 0 || logItem.getReplicationId().getValue() != 0) {
            sb.append("[Scenario = ");
            sb.append(logItem.getScenarioId().getValue());
            sb.append(", Replication = ");
            sb.append(logItem.getReplicationId().getValue());
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
