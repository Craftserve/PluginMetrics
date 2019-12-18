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

package pl.craftserve.metrics.pluginmetrics;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.bukkit.plugin.Plugin;
import pl.craftserve.metrics.pluginmetrics.entity.EntityRegistry;
import pl.craftserve.metrics.pluginmetrics.report.Endpoint;
import pl.craftserve.metrics.pluginmetrics.report.Reporter;
import pl.craftserve.metrics.pluginmetrics.report.UrlEndpoint;
import pl.craftserve.metrics.pluginmetrics.sample.Sampler;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Metrics {
    private static final Logger LOGGER = Logger.getLogger("PluginMetrics");
    private static final ServerIdResolver SERVER_ID_RESOLVER = ServerIdResolver.create();

    private final Plugin plugin;
    private final UUID serverId;
    private final EntityRegistry entityRegistry;
    private final Duration sampleInterval;
    private final Duration reportInterval;
    private final List<Endpoint> endpoints;

    private boolean running = false;
    private Sampler sampler;
    private Reporter reporter;

    protected Metrics(Plugin plugin, UUID serverId, EntityRegistry entityRegistry, Duration sampleInterval, Duration reportInterval, List<Endpoint> endpoints) {
        Preconditions.checkArgument(!endpoints.isEmpty(), "endpoints is empty!");

        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.entityRegistry = Objects.requireNonNull(entityRegistry, "entityRegistry");
        this.sampleInterval = Objects.requireNonNull(sampleInterval, "sampleInterval");
        this.reportInterval = Objects.requireNonNull(reportInterval, "reportInterval");
        this.endpoints = ImmutableList.copyOf(Objects.requireNonNull(endpoints, "endpoints"));
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public UUID getServerId() {
        return this.serverId;
    }

    public EntityRegistry getEntityRegistry() {
        return this.entityRegistry;
    }

    public void start() {
        if (this.running) {
            throw new IllegalStateException("Already running!");
        }

        LOGGER.info("Starting " + this.toString() + "...");

        try {
            this.sampler = new Sampler(this, LOGGER);
            this.reporter = new Reporter(this.sampler, LOGGER, this.endpoints, Executors.newSingleThreadExecutor());

            long sampleTaskInterval = this.toTicks(this.sampleInterval);
            Sampler.Task samplerTask = this.sampler.new Task();
            samplerTask.runTaskTimer(this.plugin, sampleTaskInterval, sampleTaskInterval);
            this.sampler.setTask(samplerTask);

            long reportTaskInterval = this.toTicks(this.reportInterval);
            Reporter.Task reporterTask = this.reporter.new Task();
            reporterTask.runTaskTimer(this.plugin, reportTaskInterval, reportTaskInterval);
            this.reporter.setTask(reporterTask);
        } finally {
            this.running = true;
        }
    }

    public void stop() {
        if (!this.running) {
            throw new IllegalStateException("Not running!");
        }

        LOGGER.info("Stopping " + this.toString() + "...");

        this.sampler.createReport().ifPresent(report -> {
            try {
                LOGGER.info(this.toString() + " is reporting queued samples...");
                this.reporter.report(report).get(10L, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.log(Level.SEVERE, "Could not report queued samples.", e);
            }
        });

        try {
            if (this.sampler != null) {
                this.sampler.getTask().ifPresent(task -> {
                    if (task.isRunning()) {
                        task.cancel();
                    }
                });
            }
            this.sampler = null;

            if (this.reporter != null) {
                this.reporter.getTask().ifPresent(task -> {
                    if (task.isRunning()) {
                        task.cancel();
                    }
                });
                this.reporter.shutdown();
            }
            this.reporter = null;
        } finally {
            this.running = false;
        }
    }

    @Override
    public String toString() {
        return "Metrics for plugin " + this.plugin.getDescription().getFullName();
    }

    private long toTicks(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        return duration.getSeconds() * 20L;
    }

    //
    // Factory Methods
    //

    public static Metrics create(Plugin plugin, UUID defaultServerId, EntityRegistry entityRegistry, MetricsProperties properties, List<Endpoint> endpoints) throws MalformedPropertiesException {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(defaultServerId, "defaultServerId");
        Objects.requireNonNull(entityRegistry, "entityRegistry");
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(endpoints, "endpoints");

        UUID serverId = properties.serverId().orElse(defaultServerId);
        Duration sampleInterval = properties.sampleInterval();
        Duration reportInterval = properties.reportInterval();

        return new Metrics(plugin, serverId, entityRegistry, sampleInterval, reportInterval, endpoints);
    }

    public static Metrics createWithDefaults(Plugin plugin) {
        try {
            return createWithDefaults(plugin, MetricsProperties.create());
        } catch (MalformedPropertiesException e) {
            throw new Error(e); // should never happen
        }
    }

    public static Metrics createWithDefaults(Plugin plugin, MetricsProperties properties) throws MalformedPropertiesException {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(properties, "properties");

        UUID serverId;
        try {
            serverId = SERVER_ID_RESOLVER.getId();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not resolve server ID for plugin metrics.", e);
            serverId = UUID.randomUUID();
        }

        EntityRegistry entityRegistry = new EntityRegistry();
        try {
            EntityRegistry.ConstantEntityList.collect(EntityRegistry.DefaultEntities.class).forEach(entityRegistry::register);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.SEVERE, "Could not collect default entities for plugin metrics.", e);
        }

        Endpoint endpoint = new UrlEndpoint(UrlEndpoint.CRAFTSERVE_METRICS);

        return create(plugin, serverId, entityRegistry, properties, Collections.singletonList(endpoint));
    }
}
