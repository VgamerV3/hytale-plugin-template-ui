package net.hytaledepot.templates.plugin.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class UiService {
  private static final String LAYOUT_RESOURCE = "Common/UI/Custom/HdUiTemplate.ui";

  private final Map<String, UiSession> sessions = new ConcurrentHashMap<>();
  private final AtomicLong actionsProcessed = new AtomicLong();

  private volatile Path dataDirectory;
  private volatile boolean layoutAvailable;
  private volatile long lastHeartbeatTick;

  public void initialize(Path dataDirectory) {
    this.dataDirectory = dataDirectory;
    this.layoutAvailable = UiService.class.getClassLoader().getResource(LAYOUT_RESOURCE) != null;
    this.lastHeartbeatTick = 0L;
  }

  public void onHeartbeat(long tick) {
    lastHeartbeatTick = tick;
    if (tick % 120 == 0) {
      sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());
    }
  }

  public String openPage(CommandContext ctx, UiPluginState state, long heartbeatTick) {
    if (!ctx.isPlayer()) {
      return "[UI] This action requires a player sender.";
    }

    Ref<EntityStore> playerEntityRef = ctx.senderAsPlayerRef();
    if (playerEntityRef == null || !playerEntityRef.isValid()) {
      return "[UI] Unable to resolve player reference for this sender.";
    }

    Store<EntityStore> store = playerEntityRef.getStore();
    EntityStore entityStore = store.getExternalData();
    World world = entityStore.getWorld();
    String actor = String.valueOf(ctx.sender().getDisplayName());

    world.execute(
        () -> {
          Player player = store.getComponent(playerEntityRef, Player.getComponentType());
          if (player == null) {
            return;
          }

          PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
          if (playerRef == null) {
            return;
          }

          PageManager pageManager = player.getPageManager();
          if (pageManager == null) {
            return;
          }

          long tick = Math.max(lastHeartbeatTick, heartbeatTick);
          recordAction(actor, "open", tick);
          pageManager.openCustomPage(playerEntityRef, store, new UiTemplatePage(state, this, playerRef));
          playerRef.sendMessage(Message.raw("[UI] Opened the UI template page."));
        });

    return "[UI] Opening the UI template page...";
  }

  public String handleAction(UiPluginState state, String sender, String action, long heartbeatTick) {
    String actor = String.valueOf(sender == null ? "unknown" : sender);
    String normalizedAction = normalizeAction(action);
    UiSession session = recordAction(actor, normalizedAction, heartbeatTick);

    if ("open".equals(normalizedAction)) {
      return "[UI] Open request accepted. sender=" + actor + ", layoutAvailable=" + layoutAvailable;
    }

    if ("close".equals(normalizedAction)) {
      return "[UI] Session closed for " + actor;
    }

    if ("toggle".equals(normalizedAction)) {
      boolean enabled = state.toggleDemoFlag();
      return "[UI] demoFlag=" + enabled + " for " + actor;
    }

    if ("heartbeat".equals(normalizedAction)) {
      return "[UI] heartbeatTicks=" + Math.max(lastHeartbeatTick, heartbeatTick) + " for " + actor;
    }

    if ("info".equals(normalizedAction)) {
      return "[UI] sessionOpen="
          + session.isOpen()
          + ", lastAction="
          + session.getLastAction()
          + ", openedAtTick="
          + session.getOpenedAtTick();
    }

    return "[UI] Unknown action='" + normalizedAction + "' (use: open, info, heartbeat, toggle, close).";
  }

  UiSession recordAction(String actor, String action, long heartbeatTick) {
    actionsProcessed.incrementAndGet();
    UiSession session = sessions.computeIfAbsent(String.valueOf(actor), key -> new UiSession(key, heartbeatTick));
    session.markAction(action, heartbeatTick);
    return session;
  }

  public String diagnosticsFor(String sender) {
    String actor = String.valueOf(sender == null ? "unknown" : sender);
    UiSession session = sessions.get(actor);
    String directory = dataDirectory == null ? "unset" : dataDirectory.toString();
    if (session == null) {
      return "session=none, openSessions=" + openSessionCount() + ", actionsProcessed=" + actionsProcessed.get() + ", dataDirectory=" + directory;
    }

    return "sessionOpen="
        + session.isOpen()
        + ", lastUiAction="
        + session.getLastAction()
        + ", openedAtTick="
        + session.getOpenedAtTick()
        + ", openSessions="
        + openSessionCount()
        + ", actionsProcessed="
        + actionsProcessed.get()
        + ", heartbeatTicks="
        + lastHeartbeatTick
        + ", dataDirectory="
        + directory;
  }

  public int openSessionCount() {
    int count = 0;
    for (UiSession session : sessions.values()) {
      if (session.isOpen()) {
        count++;
      }
    }
    return count;
  }

  public boolean isLayoutAvailable() {
    return layoutAvailable;
  }

  public long getLastHeartbeatTick() {
    return lastHeartbeatTick;
  }

  public void shutdown() {
    sessions.clear();
  }

  private static String normalizeAction(String action) {
    String normalized = String.valueOf(action == null ? "" : action).trim().toLowerCase();
    return normalized.isEmpty() ? "open" : normalized;
  }
}
