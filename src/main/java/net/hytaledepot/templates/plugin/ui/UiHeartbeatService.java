package net.hytaledepot.templates.plugin.ui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class UiHeartbeatService {
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "hd-ui-heartbeat");
            thread.setDaemon(true);
            return thread;
          });

  private final AtomicLong heartbeatTicks = new AtomicLong();
  private volatile ScheduledFuture<?> heartbeatTask;

  public void start(Consumer<Long> onTick, Consumer<Exception> onError) {
    stop();
    heartbeatTask =
        scheduler.scheduleAtFixedRate(
            () -> {
              try {
                long tick = heartbeatTicks.incrementAndGet();
                onTick.accept(tick);
              } catch (Exception exception) {
                onError.accept(exception);
              }
            },
            1,
            1,
            TimeUnit.SECONDS);
  }

  public void stop() {
    if (heartbeatTask != null) {
      heartbeatTask.cancel(true);
      heartbeatTask = null;
    }
  }

  public void shutdown() {
    stop();
    scheduler.shutdownNow();
  }

  public long ticks() {
    return heartbeatTicks.get();
  }

  public boolean isActive() {
    return heartbeatTask != null && !heartbeatTask.isCancelled() && !heartbeatTask.isDone();
  }
}
