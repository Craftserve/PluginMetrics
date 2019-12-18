/*
 * Copyright 2019 Aleksander Jagiełło <themolkapl@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.craftserve.metrics.pluginmetrics.util;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class TrackedBukkitRunnable extends BukkitRunnable {
    private static final int MAGIC = -1;

    private AtomicInteger taskId = new AtomicInteger(MAGIC);

    public synchronized boolean isRunning() {
        return this.getTaskId() != MAGIC;
    }

    @Override
    public synchronized boolean isCancelled() throws IllegalStateException {
        return !this.isRunning();
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        this.resetTaskId();
    }

    @Override
    public synchronized BukkitTask runTask(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        return this.track(super.runTask(plugin));
    }

    @Override
    public synchronized BukkitTask runTaskAsynchronously(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        return this.track(super.runTaskAsynchronously(plugin));
    }

    @Override
    public synchronized BukkitTask runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        return this.track(super.runTaskLater(plugin, delay));
    }

    @Override
    public synchronized BukkitTask runTaskLaterAsynchronously(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        return this.track(super.runTaskLaterAsynchronously(plugin, delay));
    }

    @Override
    public synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        return this.track(super.runTaskTimer(plugin, delay, period));
    }

    @Override
    public synchronized BukkitTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        return this.track(super.runTaskTimerAsynchronously(plugin, delay, period));
    }

    @Override
    public synchronized int getTaskId() throws IllegalStateException {
        return this.taskId.get();
    }

    private void resetTaskId() {
        this.taskId.set(MAGIC);
    }

    private <T extends BukkitTask> T track(T task) {
        Objects.requireNonNull(task);
        this.taskId.set(task.getTaskId());
        return task;
    }
}
