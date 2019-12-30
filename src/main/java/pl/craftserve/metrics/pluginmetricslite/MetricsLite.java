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

package pl.craftserve.metrics.pluginmetricslite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetricsLite {
    private static final Logger LOGGER;
    private static final Gson GSON;
    private static final ServerIdResolver SERVER_ID_RESOLVER;
    private static final UrlEndpoint ENDPOINT;
    private static final Duration INTERVAL;

    static {
        LOGGER = Logger.getLogger(MetricsLite.class.getName());
        GSON = new GsonBuilder().create();
        SERVER_ID_RESOLVER = ServerIdResolver.create();
        ENDPOINT = new UrlEndpoint(UrlEndpoint.CRAFTSERVE_METRICS);
        INTERVAL = Duration.ofMinutes(1L);
    }

    private static MetricsLite globalMetrics;

    /**
     * Returns current instance of this class. It is highly unrecommended to use
     * this method.
     * @return Instance of this class.
     * @throws IllegalStateException Whether metrics is not running.
     */
    @Deprecated
    public static MetricsLite getMetrics() {
        MetricsLite instance = globalMetrics;
        if (instance == null) {
            throw new IllegalStateException(MetricsLite.class.getSimpleName() + " didn't start yet!");
        }

        return instance;
    }

    /**
     * Start metrics for the given plugin.
     * @param plugin Owner of the metric.
     * @throws IllegalStateException Whether metric for this plugin is already running.
     */
    public static void start(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (globalMetrics == null) {
            globalMetrics = produceMetrics();
        }
        MetricsLite metrics = getMetrics();

        List<Plugin> affectedPlugins = metrics.affectedPlugins;
        if (affectedPlugins.contains(plugin)) {
            throw new IllegalStateException("Already started for " + plugin.toString());
        }

        boolean wasEmpty = affectedPlugins.isEmpty();
        affectedPlugins.add(plugin);

        if (wasEmpty) {
            // no plugins were affected before, start metrics
            metrics.start();
        }
    }

    /**
     * Stop metrics for the given plugin.
     * @param plugin Owner of the metric.
     * @throws IllegalStateException Whether metric for this plugin is not running.
     */
    public static void stop(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        MetricsLite metrics = getMetrics();

        List<Plugin> affectedPlugins = metrics.affectedPlugins;
        if (!affectedPlugins.contains(plugin)) {
            throw new IllegalStateException("Not started for " + plugin.toString());
        }

        affectedPlugins.remove(plugin);
        boolean empty = affectedPlugins.isEmpty();

        if (empty) {
            // no plugins affected, stop metrics
            metrics.stop();
            globalMetrics = null;
        }
    }

    /**
     * Stop metrics for the given plugin.
     * @param plugin Owner of the metric.
     */
    public static void stopIfRunning(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (isRunning(plugin)) {
            stop(plugin);
        }
    }

    /**
     * Is metric for the given plugin?
     * @param plugin Owner of the metric.
     * @return Whether metric for the given plugin is running.
     */
    public static boolean isRunning(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        MetricsLite metrics = globalMetrics;
        return metrics != null && metrics.affectedPlugins.contains(plugin);
    }

    private static MetricsLite produceMetrics() {
        return new MetricsLite(LOGGER, GSON, SERVER_ID_RESOLVER, ENDPOINT, INTERVAL);
    }

    //
    // Non-static context
    //

    private final List<Plugin> affectedPlugins = new CopyOnWriteArrayList<>();
    private final DefaultEntities defaultEntities = new DefaultEntities();

    private final Logger logger;
    private final Gson gson;
    private final ServerIdResolver serverIdResolver;
    private final UrlEndpoint endpoint;
    private final Duration interval;

    private boolean running;
    private Timer timer;

    public MetricsLite(Logger logger, Gson gson, ServerIdResolver serverIdResolver, UrlEndpoint endpoint, Duration interval) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.gson = Objects.requireNonNull(gson, "gson");
        this.serverIdResolver = Objects.requireNonNull(serverIdResolver, "serverIdResolver");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.interval = Objects.requireNonNull(interval, "interval");
    }

    public void start() {
        if (this.running) {
            throw new IllegalStateException("Already running!");
        } else if (this.affectedPlugins.isEmpty()) {
            throw new IllegalStateException("No plugins attached!");
        }

        try {
            this.logger.info("Starting " + this.toString() + "...");

            Server server = this.affectedPlugins.get(0).getServer();
            UUID serverId = this.resolveServerId();
            Reporter reporter = new Reporter(server, serverId);
            long msInterval = this.interval.toMillis();

            this.timer = new Timer("Metrics-Lite-Submitter", false);
            this.timer.scheduleAtFixedRate(reporter, msInterval, msInterval);
        } finally {
            this.running = true;
        }
    }

    public void stop() {
        if (!this.running) {
            throw new IllegalStateException("Not running!");
        } else if (!this.affectedPlugins.isEmpty()) {
            throw new IllegalStateException("Plugins are still attached!");
        }

        try {
            this.logger.info("Stopping " + this.toString() + "...");
            if (this.timer != null) {
                this.timer.cancel();
            }
        } finally {
            this.running = false;
        }
    }

    public List<Plugin> getAffectedPlugins() {
        return Collections.unmodifiableList(this.affectedPlugins);
    }

    @Override
    public String toString() {
        return "Metrics Lite";
    }

    private UUID resolveServerId() {
        try {
            return this.serverIdResolver.getId();
        } catch (IOException e) {
            this.logger.log(Level.SEVERE, "Could not resolve server ID for " + this.toString(), e);
            return UUID.randomUUID();
        }
    }

    class Reporter extends TimerTask {
        private final Server server;
        private final UUID serverId;

        Reporter(Server server, UUID serverId) {
            this.server = Objects.requireNonNull(server, "server");
            this.serverId = Objects.requireNonNull(serverId, "serverId");
        }

        @Override
        public void run() {
            logger.fine("Collecting metric for " + MetricsLite.this.toString() + "...");

            Map<NamespacedKey, Object> data = this.collectData(this.server);
            if (data.isEmpty()) {
                return;
            }

            UUID reportId = UUID.randomUUID();
            Instant now = Instant.now();

            JsonObject payload = new JsonObject();
            this.serializeData(payload, data);

            JsonObject report = new JsonObject();
            report.addProperty("id", reportId.toString());
            report.addProperty("server_id", this.serverId.toString());
            report.addProperty("taken_at", now.toString());
            report.add("payload", payload);

            logger.fine("Submitting report for " + MetricsLite.this.toString());
            try {
                endpoint.submit(report);
            } catch (Throwable throwable) {
                logger.log(Level.SEVERE, "Could not submit report " + reportId.toString() + " for " + MetricsLite.this.toString(), throwable);
            }
        }

        private Map<NamespacedKey, Object> collectData(Server server) {
            Objects.requireNonNull(server, "server");

            Map<NamespacedKey, Object> data = new LinkedHashMap<>(512);
            try {
                defaultEntities.append(data, MetricsLite.this, server);
            } catch (Throwable throwable) {
                logger.log(Level.SEVERE, "Could not create default data for " + MetricsLite.this.toString(), throwable);
            }

            MetricSubmitEvent event = new MetricSubmitEvent(!server.isPrimaryThread(), data);
            server.getPluginManager().callEvent(event);

            return event.getData();
        }

        private void serializeData(JsonObject payload, Map<NamespacedKey, Object> data) {
            Objects.requireNonNull(payload, "payload");
            Objects.requireNonNull(data, "data");

            for (Map.Entry<NamespacedKey, Object> entry : data.entrySet()) {
                JsonElement serialized = null;
                try {
                    serialized = gson.toJsonTree(entry.getValue());
                } catch (Throwable throwable) {
                    logger.log(Level.SEVERE, "Could not serialize data for " + MetricsLite.this.toString(), throwable);
                }

                if (serialized == null || serialized.isJsonNull()) {
                    continue;
                }

                NamespacedKey key = entry.getKey();

                JsonObject category = this.getOrCreateCategory(payload, key.getNamespace());
                category.add(key.getKey(), serialized);
            }
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
    }
}
