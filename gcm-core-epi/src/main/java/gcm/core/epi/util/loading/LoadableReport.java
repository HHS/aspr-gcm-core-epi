package gcm.core.epi.util.loading;

import gcm.core.epi.plugin.Plugin;
import nucleus.ReportContext;
import nucleus.ReportId;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface LoadableReport extends ReportId {
    Supplier<Consumer<ReportContext>> getSupplier(ReportWrapperItem reportWrapperItem, List<Plugin> pluginList);
}
