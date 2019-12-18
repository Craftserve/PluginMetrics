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

package pl.craftserve.metrics.pluginmetrics.sample;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import pl.craftserve.metrics.pluginmetrics.Metrics;
import pl.craftserve.metrics.pluginmetrics.entity.Entity;
import pl.craftserve.metrics.pluginmetrics.report.Report;
import pl.craftserve.metrics.pluginmetrics.util.TrackedBukkitRunnable;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Sampler {
    private final Metrics metrics;
    private final Logger logger;
    private final List<Sample> samples = new ArrayList<>(512);

    private Task task;

    public Sampler(Metrics metrics, Logger logger) {
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Metrics getMetrics() {
        return this.metrics;
    }

    public List<Sample> getSamples() {
        return Collections.unmodifiableList(this.samples);
    }

    public Optional<Task> getTask() {
        return Optional.ofNullable(this.task);
    }

    public void setTask(Task task) {
        if (this.task != null && this.task.isRunning()) {
            throw new IllegalStateException("Sampler task is running!");
        }

        this.task = task;
    }

    public boolean offer(Sample sample) {
        Objects.requireNonNull(sample, "sample");
        return this.samples.add(sample);
    }

    public Sample takeSample(BiConsumer<Entity<?>, Throwable> errorConsumer, boolean persist) throws IOException {
        Objects.requireNonNull(errorConsumer, "errorConsumer");

        JsonObject payload = new JsonObject();

        for (Entity entity : this.metrics.getEntityRegistry().values()) {
            JsonElement serialized = null;
            try {
                serialized = entity.createSerializedRecord(this.metrics);
            } catch (Throwable throwable) {
                errorConsumer.accept(entity, throwable);
            }

            if (serialized == null || serialized.isJsonNull()) {
                continue;
            }

            NamespacedKey key = entity.getKey();

            JsonObject category = this.getOrCreateCategory(payload, key.getNamespace());
            category.add(key.getKey(), serialized);
        }

        UUID uniqueId = UUID.randomUUID();
        UUID serverId = this.metrics.getServerId();
        Instant takenAt = Instant.now();
        Sample sample = new Sample(uniqueId, serverId, takenAt, payload);

        if (persist) {
            this.offer(sample);
        }

        return sample;
    }

    private JsonObject getOrCreateCategory(JsonObject parent, String name) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(name, "name");

        if (parent.has(name)) {
            JsonElement category = parent.get(name);
            if (category instanceof JsonObject) {
                return (JsonObject) category;
            }
        }

        JsonObject category = new JsonObject();
        parent.add(name, category);

        return category;
    }

    public List<Sample> flush() {
        List<Sample> removed = ImmutableList.copyOf(this.samples);
        this.samples.clear();
        return removed;
    }

    public Optional<Report> createReport() {
        List<Sample> samples = this.flush();
        if (samples.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Report(UUID.randomUUID(), samples));
    }

    public class Task extends TrackedBukkitRunnable {
        @Override
        public void run() {
            Sampler sampler = Sampler.this;
            try {
                sampler.takeSample((entity, throwable) -> {
                    sampler.logger.log(Level.SEVERE, "Could not take sample for " + entity.getKey().toString() + ".", throwable);
                }, true);
            } catch (IOException e) {
                sampler.logger.log(Level.SEVERE, "IOException has occurred.", e);
            }
        }
    }
}
