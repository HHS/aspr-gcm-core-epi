package gcm.core.epi.util.loading;

import gcm.core.epi.plugin.Plugin;
import gcm.experiment.ExperimentExecutor;

import java.nio.file.Path;
import java.util.List;

public interface LoadableReport {
    void load(ExperimentExecutor experimentExecutor, Path path, ReportWrapperItem reportWrapperItem, List<Plugin> pluginList);
}
