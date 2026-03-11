package net.hytaledepot.templates.plugin.ui;

public final class UiSession {
  private final String playerName;
  private long openedAtTick;
  private String lastAction;
  private long lastUpdatedAtEpochMillis;
  private boolean open;

  public UiSession(String playerName, long openedAtTick) {
    this.playerName = String.valueOf(playerName);
    this.openedAtTick = openedAtTick;
    this.lastAction = "open";
    this.lastUpdatedAtEpochMillis = System.currentTimeMillis();
    this.open = true;
  }

  public String getPlayerName() {
    return playerName;
  }

  public long getOpenedAtTick() {
    return openedAtTick;
  }

  public String getLastAction() {
    return lastAction;
  }

  public long getLastUpdatedAtEpochMillis() {
    return lastUpdatedAtEpochMillis;
  }

  public boolean isOpen() {
    return open;
  }

  public void markAction(String action, long heartbeatTick) {
    this.lastAction = String.valueOf(action);
    this.lastUpdatedAtEpochMillis = System.currentTimeMillis();
    if ("open".equalsIgnoreCase(action)) {
      this.openedAtTick = heartbeatTick;
      this.open = true;
    }
    if ("close".equalsIgnoreCase(action)) {
      this.open = false;
    }
  }
}
