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

import com.google.gson.JsonArray;
import org.bukkit.NamespacedKey;
import pl.craftserve.metrics.pluginmetrics.Metrics;
import pl.craftserve.metrics.pluginmetrics.RecordFactory;

public abstract class ArrayEntity<T> extends Entity<T> {
    public ArrayEntity(NamespacedKey key, RecordFactory<T> recordFactory) {
        super(key, recordFactory);
    }

    @Override
    public abstract JsonArray createSerializedRecord(Metrics metrics) throws Throwable;
}
