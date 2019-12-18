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

package pl.craftserve.metrics.pluginmetrics.entity;

import com.google.common.collect.ForwardingMap;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import pl.craftserve.metrics.pluginmetrics.RecordFactory;
import pl.craftserve.metrics.pluginmetrics.SystemPropertyFactory;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Registry collecting all entities creating records.
 */
public class EntityRegistry extends ForwardingMap<NamespacedKey, Entity> {
    /**
     * Registry containing all entities indexed by their unique key.
     */
    private final Map<NamespacedKey, Entity> registry = new LinkedHashMap<>(512);

    @Override
    protected Map<NamespacedKey, Entity> delegate() {
        // Protect against all write operations.
        return Collections.unmodifiableMap(this.registry);
    }

    /**
     * Register the given entity into this registry.
     * @param entity Entity to be registered.
     */
    public void register(Entity entity) {
        Objects.requireNonNull(entity, "entity");

        NamespacedKey key = entity.getKey();
        if (this.registry.containsKey(key)) {
            throw new IllegalStateException(key.toString() + " is already registered!");
        }

        this.registry.put(key, entity);
    }

    /**
     * Base interface for all entities defined as constants.
     */
    public interface ConstantEntityList {
        /**
         * Collect all entities from the given list. All entities must inherit
         * from the base {@link Entity} class.
         * @param list List of entities defined as constants.
         * @return List of entity object instances.
         * @throws ReflectiveOperationException Whether instance of a constant
         * cannot be established.
         */
        static List<Entity> collect(Class<? extends ConstantEntityList> list) throws ReflectiveOperationException {
            Objects.requireNonNull(list, "list");

            List<Entity> entities = new ArrayList<>(512);
            for (Field field : list.getFields()) {
                if (Entity.class.isAssignableFrom(field.getType())) {
                    entities.add((Entity) field.get(null));
                }
            }

            return entities;
        }
    }

    /**
     * Interface Collecting all default entities.
     */
    public interface DefaultEntities extends ConstantEntityList,
            Bukkit,
            Craftserve,
            Java,
            System {
    }

    /**
     * Bukkit-related entities, such as online-mode and software version.
     */
    public interface Bukkit extends ConstantEntityList {
        Entity<String> SERVER_NAME = new StringEntity(bukkit("server_name"),
                RecordFactory.forServer(Server::getName));
        Entity<String> SERVER_VERSION = new StringEntity(bukkit("server_version"),
                RecordFactory.forServer(Server::getVersion));
        Entity<String> VERSION = new StringEntity(bukkit("version"),
                RecordFactory.forServer(Server::getBukkitVersion));

        Entity<Number> ONLINE_COUNT = new NumberEntity(bukkit("online_count"),
                RecordFactory.forServer(server -> server.getOnlinePlayers().size()));
        Entity<Number> SLOTS = new NumberEntity(bukkit("slots"),
                RecordFactory.forServer(Server::getMaxPlayers));
        Entity<Number> VIEW_DISTANCE = new NumberEntity(bukkit("view_distance"),
                RecordFactory.forServer(Server::getViewDistance));
        Entity<Number> PLUGIN_COUNT = new NumberEntity(bukkit("plugin_count"),
                RecordFactory.forServer(server -> server.getPluginManager().getPlugins().length));
        Entity<Number> WORLD_COUNT = new NumberEntity(bukkit("world_count"),
                RecordFactory.forServer(server -> server.getWorlds().size()));

        Entity<Boolean> HAS_WHITELIST = new BooleanEntity(bukkit("has_whitelist"),
                RecordFactory.forServer(Server::hasWhitelist));
        Entity<Boolean> IS_HARDCORE = new BooleanEntity(bukkit("is_hardcore"),
                RecordFactory.forServer(Server::isHardcore));
        Entity<Boolean> IS_ONLINE_MODE = new BooleanEntity(bukkit("is_online_mode"),
                RecordFactory.forServer(Server::getOnlineMode));

        Entity<String> PLUGIN_NAME = new StringEntity(bukkit("plugin_name"),
                RecordFactory.forPluginDescriptionFile(PluginDescriptionFile::getName));
        Entity<String> PLUGIN_VERSION = new StringEntity(bukkit("plugin_version"),
                RecordFactory.forPluginDescriptionFile(PluginDescriptionFile::getVersion));
        Entity<String> PLUGIN_API_VERSION = new StringEntity(bukkit("plugin_api_version"),
                RecordFactory.forPluginDescriptionFile(PluginDescriptionFile::getAPIVersion));

        static NamespacedKey bukkit(String key) {
            Objects.requireNonNull(key, "key");
            return new NamespacedKey(NamespacedKey.BUKKIT, key);
        }
    }

    /**
     * Craftserve hosting-related entities.
     */
    public interface Craftserve extends ConstantEntityList {
        Entity<Boolean> IS_HOSTED_ON = new BooleanEntity(csrv("is_hosted_on"), metrics -> {
            String hostName = InetAddress.getLocalHost().getHostName();
            return hostName.toLowerCase(Locale.US).endsWith(".craftserve.pl");
        });

        static NamespacedKey csrv(String key) {
            Objects.requireNonNull(key, "key");
            return new NamespacedKey("craftserve", key);
        }
    }

    /**
     * Java Virtual Machine-related entities.
     */
    public interface Java extends ConstantEntityList {
        Entity<String> JAVA_VENDOR = new StringEntity(java("vendor"),
                SystemPropertyFactory.create("java.vendor"));
        Entity<String> JAVA_VENDOR_URL = new StringEntity(java("vendor_url"),
                SystemPropertyFactory.create("java.vendor.url"));
        Entity<String> JAVA_VERSION = new StringEntity(java("version"),
                SystemPropertyFactory.create("java.version"));

        Entity<String> JAVA_RUNTIME_NAME = new StringEntity(java("runtime_name"),
                SystemPropertyFactory.create("java.runtime.name"));
        Entity<String> JAVA_RUNTIME_VERSION = new StringEntity(java("runtime_version"),
                SystemPropertyFactory.create("java.runtime.version"));

        static NamespacedKey java(String key) {
            Objects.requireNonNull(key, "key");
            return new NamespacedKey("java", key);
        }
    }

    /**
     * System-related entities, such as memory usage and OS version.
     */
    public interface System extends ConstantEntityList {
        Entity<String> OS_ARCH = new StringEntity(system("os_arch"),
                SystemPropertyFactory.create("os.arch"));
        Entity<String> OS_NAME = new StringEntity(system("os_name"),
                SystemPropertyFactory.create("os.name"));
        Entity<String> OS_VERSION = new StringEntity(system("os_version"),
                SystemPropertyFactory.create("os.version"));

        Entity<Number> AVAILABLE_PROCESSORS = new NumberEntity(system("available_processors"),
                RecordFactory.forRuntime(Runtime::availableProcessors));
        Entity<Number> FREE_MEMORY = new NumberEntity(system("free_memory"),
                RecordFactory.forRuntime(Runtime::freeMemory));
        Entity<Number> TOTAL_MEMORY = new NumberEntity(system("total_memory"),
                RecordFactory.forRuntime(Runtime::totalMemory));
        Entity<Number> MAX_MEMORY = new NumberEntity(system("max_memory"),
                RecordFactory.forRuntime(Runtime::maxMemory));

        static NamespacedKey system(String key) {
            Objects.requireNonNull(key, "key");
            return new NamespacedKey("system", key);
        }
    }
}
