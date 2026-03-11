package net.hytaledepot.templates.plugin.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;

public final class UiStatusCommand extends AbstractCommand {
  private final UiPluginState state;
  private final UiService uiService;
  private final UiHeartbeatService heartbeatService;

  public UiStatusCommand(UiPluginState state, UiService uiService, UiHeartbeatService heartbeatService) {
    super("hduistatus", "Shows runtime diagnostics for the UI template.");
    setAllowsExtraArguments(true);
    this.state = state;
    this.uiService = uiService;
    this.heartbeatService = heartbeatService;
  }

  @Override
  protected CompletableFuture<Void> execute(CommandContext ctx) {
    state.incrementStatusRequests();
    String sender = String.valueOf(ctx.sender().getDisplayName());

    long uptimeSeconds = 0L;
    if (state.getStartedAtEpochMillis() > 0L) {
      uptimeSeconds = Math.max(0L, (System.currentTimeMillis() - state.getStartedAtEpochMillis()) / 1000L);
    }

    String line =
        "[UI] lifecycle="
            + state.getLifecycle()
            + ", uptime="
            + uptimeSeconds
            + "s"
            + ", heartbeatTicks="
            + heartbeatService.ticks()
            + ", heartbeatActive="
            + heartbeatService.isActive()
            + ", setupCompleted="
            + state.isSetupCompleted()
            + ", demoFlag="
            + state.isDemoFlagEnabled()
            + ", openSessions="
            + uiService.openSessionCount()
            + ", errors="
            + state.getErrorCount()
            + ", layoutAvailable="
            + uiService.isLayoutAvailable();

    ctx.sendMessage(Message.raw(line));
    ctx.sendMessage(Message.raw("[UI] sender=" + sender + ", " + uiService.diagnosticsFor(sender)));
    return CompletableFuture.completedFuture(null);
  }
}
