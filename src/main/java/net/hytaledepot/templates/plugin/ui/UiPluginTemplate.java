package net.hytaledepot.templates.plugin.ui;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.concurrent.CompletableFuture;

public final class UiPluginTemplate extends JavaPlugin {
  private final UiPluginState state = new UiPluginState();
  private final UiService uiService = new UiService();
  private final UiHeartbeatService heartbeatService = new UiHeartbeatService();

  public UiPluginTemplate(JavaPluginInit init) {
    super(init);
  }

  @Override
  public CompletableFuture<Void> preLoad() {
    state.setLifecycle(UiPluginLifecycle.PRELOADING);
    getLogger().atInfo().log("[UI] preLoad -> %s", getIdentifier());
    return CompletableFuture.completedFuture(null);
  }

  @Override
  protected void setup() {
    state.setLifecycle(UiPluginLifecycle.SETTING_UP);
    state.setDataDirectory(getDataDirectory().toString());

    uiService.initialize(getDataDirectory());

    getCommandRegistry().registerCommand(new UiOpenCommand(state, uiService, heartbeatService));
    getCommandRegistry().registerCommand(new UiStatusCommand(state, uiService, heartbeatService));

    state.markSetupCompleted();
    state.setLifecycle(UiPluginLifecycle.READY);
  }

  @Override
  protected void start() {
    state.setLifecycle(UiPluginLifecycle.RUNNING);
    state.setStartedAtEpochMillis(System.currentTimeMillis());

    heartbeatService.start(
        tick -> {
          uiService.onHeartbeat(tick);
          if (tick % 60 == 0) {
            getLogger().atInfo().log("[UI] heartbeat=%d", tick);
          }
        },
        exception -> {
          state.setLifecycle(UiPluginLifecycle.FAILED);
          state.incrementErrorCount();
          getLogger().atInfo().log("[UI] heartbeat task failed: %s", exception.getMessage());
        });

    getTaskRegistry().registerTask(CompletableFuture.completedFuture(null));
  }

  @Override
  protected void shutdown() {
    state.setLifecycle(UiPluginLifecycle.STOPPING);
    heartbeatService.shutdown();
    uiService.shutdown();
    state.setLifecycle(UiPluginLifecycle.STOPPED);
  }
}
