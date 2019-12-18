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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.bukkit.NamespacedKey;
import pl.craftserve.metrics.pluginmetrics.Metrics;
import pl.craftserve.metrics.pluginmetrics.RecordFactory;

import java.util.Objects;
import java.util.Optional;

public abstract class Entity<T> {
    private final NamespacedKey key;
    private final RecordFactory<T> recordFactory;

    public Entity(NamespacedKey key, RecordFactory<T> recordFactory) {
        this.key = Objects.requireNonNull(key, "key");
        this.recordFactory = Objects.requireNonNull(recordFactory, "recordFactory");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return Objects.equals(key, entity.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    public NamespacedKey getKey() {
        return this.key;
    }

    public Optional<T> createRecord(Metrics metrics) throws Throwable {
        Objects.requireNonNull(metrics, "metrics");
        return Optional.ofNullable(this.recordFactory.get(metrics));
    }

    public JsonElement createSerializedRecord(Metrics metrics) throws Throwable {
        return this.createRecord(metrics)
                .map(this::serialize)
                .orElse(JsonNull.INSTANCE);
    }

    protected abstract JsonElement serialize(T record);
}
