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

import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Called when metrics are about to be submitted.
 */
public class MetricSubmitEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final Map<NamespacedKey, Object> data;

    public MetricSubmitEvent(boolean isAsync, Map<NamespacedKey, Object> data) {
        super(isAsync);
        this.data = Collections.synchronizedMap(Objects.requireNonNull(data, "data"));
    }

    public Map<NamespacedKey, Object> getData() {
        return this.data;
    }
}
