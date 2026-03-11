package net.hytaledepot.templates.plugin.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.util.concurrent.CompletableFuture;

public final class UiOpenCommand extends AbstractCommand {
  private final UiPluginState state;
  private final UiService uiService;
  private final UiHeartbeatService heartbeatService;

  public UiOpenCommand(UiPluginState state, UiService uiService, UiHeartbeatService heartbeatService) {
    super("hdui", "Runs UI demo actions: open, info, heartbeat, toggle, close.");
    setAllowsExtraArguments(true);
    this.state = state;
    this.uiService = uiService;
    this.heartbeatService = heartbeatService;
  }

  @Override
  protected CompletableFuture<Void> execute(CommandContext ctx) {
    state.incrementUiRequests();

    if (state.getLifecycle() != UiPluginLifecycle.RUNNING && state.getLifecycle() != UiPluginLifecycle.READY) {
      ctx.sendMessage(Message.raw("[UI] Plugin is not ready yet. Current lifecycle=" + state.getLifecycle()));
      return CompletableFuture.completedFuture(null);
    }

    String action = parseAction(ctx.getInputString());
    if (("open".equals(action) || "close".equals(action) || "toggle".equals(action)) && !ctx.isPlayer()) {
      ctx.sendMessage(Message.raw("[UI] This action requires a player sender."));
      return CompletableFuture.completedFuture(null);
    }

    String result;
    if ("open".equals(action)) {
      result = uiService.openPage(ctx, state, heartbeatService.ticks());
    } else {
      String sender = String.valueOf(ctx.sender().getDisplayName());
      result = uiService.handleAction(state, sender, action, heartbeatService.ticks());
    }
    ctx.sendMessage(Message.raw(result));
    return CompletableFuture.completedFuture(null);
  }

  private static String parseAction(String input) {
    String normalized = String.valueOf(input == null ? "" : input).trim();
    if (normalized.isEmpty()) {
      return "open";
    }
    String[] parts = normalized.split("\\s+");
    String first = parts[0].toLowerCase();
    if (first.startsWith("/")) {
      first = first.substring(1);
    }
    if ((parts.length == 1) && ("hdui".equals(first) || "ui".equals(first))) {
      return "open";
    }
    if (parts.length > 1 && first.startsWith("hd")) {
      return parts[1].toLowerCase();
    }
    return first;
  }
}
