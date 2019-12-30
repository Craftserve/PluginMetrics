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

import com.google.gson.annotations.SerializedName;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class DefaultEntities {
    public void append(Map<NamespacedKey, Object> data, MetricsLite metrics, Server server) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(metrics, "metrics");
        Objects.requireNonNull(server, "server");

        Runtime runtime = Runtime.getRuntime();

        this.appendBukkit(data, metrics, server);
        this.appendCraftserve(data);
        this.appendJava(data);
        this.appendSystem(data, runtime);
    }

    //
    // Bukkit Entities
    //

    private static final String BUKKIT_NAMESPACE = NamespacedKey.BUKKIT;

    /**
     * Bukkit-related entities, such as online-mode and software version.
     */
    private void appendBukkit(Map<NamespacedKey, Object> data, MetricsLite metrics, Server server) {
        data.put(bukkit("server_name"), server.getName());
        data.put(bukkit("server_version"), server.getVersion());
        data.put(bukkit("version"), server.getBukkitVersion());

        data.put(bukkit("online_count"), server.getOnlinePlayers().size());
        data.put(bukkit("slots"), server.getMaxPlayers());
        data.put(bukkit("view_distance"), server.getViewDistance());

        data.put(bukkit("has_whitelist"), server.hasWhitelist());
        data.put(bukkit("is_hardcore"), server.isHardcore());
        data.put(bukkit("is_online_mode"), server.getOnlineMode());

        Collection<PluginInfo> affectedPlugins = metrics.getAffectedPlugins().stream()
                .map(affectedPlugin -> new PluginInfo(affectedPlugin.getDescription()))
                .collect(Collectors.toList());

        if (!affectedPlugins.isEmpty()) {
            data.put(bukkit("affected_plugins"), affectedPlugins);
        }
    }

    private static NamespacedKey bukkit(String key) {
        Objects.requireNonNull(key, "key");
        return new NamespacedKey(BUKKIT_NAMESPACE, key);
    }

    private static class PluginInfo {
        @SerializedName("name") final String name;
        @SerializedName("version") final String version;
        @SerializedName("api_version") final String apiVersion;

        private PluginInfo(PluginDescriptionFile pluginDescriptionFile) {
            Objects.requireNonNull(pluginDescriptionFile, "pluginDescriptionFile");
            this.name = pluginDescriptionFile.getName();
            this.version = pluginDescriptionFile.getVersion();
            this.apiVersion = pluginDescriptionFile.getAPIVersion();
        }
    }

    //
    // Craftserve Entities
    //

    private static final String CRAFTSERVE_NAMESPACE = "craftserve";

    /**
     * Craftserve hosting-related entities.
     */
    private void appendCraftserve(Map<NamespacedKey, Object> data) {
        boolean isHostedOn;
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            isHostedOn = hostName.toLowerCase(Locale.US).endsWith(".craftserve.pl");
        } catch (UnknownHostException e) {
            isHostedOn = false;
        }

        data.put(craftserve("is_hosted_on"), isHostedOn);
    }

    private static NamespacedKey craftserve(String key) {
        Objects.requireNonNull(key, "key");
        return new NamespacedKey(CRAFTSERVE_NAMESPACE, key);
    }

    //
    // Java Entities
    //

    private static final String JAVA_NAMESPACE = "java";

    /**
     * Java Virtual Machine-related entities.
     */
    private void appendJava(Map<NamespacedKey, Object> data) {
        data.put(java("vendor"), System.getProperty("java.vendor"));
        data.put(java("vendor_url"), System.getProperty("java.vendor.url"));
        data.put(java("version"), System.getProperty("java.version"));

        data.put(java("runtime_name"), System.getProperty("java.runtime.name"));
        data.put(java("runtime_version"), System.getProperty("java.runtime.version"));
    }

    private static NamespacedKey java(String key) {
        Objects.requireNonNull(key, "key");
        return new NamespacedKey(JAVA_NAMESPACE, key);
    }

    //
    // System Entities
    //

    private static final String SYSTEM_NAMESPACE = "system";

    /**
     * System-related entities, such as memory usage and OS version.
     */
    private void appendSystem(Map<NamespacedKey, Object> data, Runtime runtime) {
        data.put(system("os_arch"), System.getProperty("os.arch"));
        data.put(system("os_name"), System.getProperty("os.name"));
        data.put(system("os_version"), System.getProperty("os.version"));

        data.put(system("available_processors"), runtime.availableProcessors());
        data.put(system("free_memory"), runtime.freeMemory());
        data.put(system("total_memory"), runtime.totalMemory());
        data.put(system("max_memory"), runtime.maxMemory());
    }

    private static NamespacedKey system(String key) {
        Objects.requireNonNull(key, "key");
        return new NamespacedKey(SYSTEM_NAMESPACE, key);
    }
}
