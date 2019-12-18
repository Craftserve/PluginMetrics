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

import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

public class Sample {
    private final UUID uniqueId;
    private final UUID serverId;
    private final Instant takenAt;
    private final JsonObject payload;

    public Sample(UUID uniqueId, UUID serverId, Instant takenAt, JsonObject payload) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.takenAt = Objects.requireNonNull(takenAt, "takenAt");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sample sample = (Sample) o;
        return Objects.equals(uniqueId, sample.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public UUID getServerId() {
        return this.serverId;
    }

    public Instant getTakenAt() {
        return this.takenAt;
    }

    public JsonObject getPayload() {
        return this.payload;
    }

    public void serialize(JsonObject object) {
        Objects.requireNonNull(object, "object");
        object.addProperty("id", this.uniqueId.toString());
        object.addProperty("server_id", this.serverId.toString());
        object.addProperty("taken_at", this.takenAt.toString());
        object.add("payload", this.payload);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Sample.class.getSimpleName() + "[", "]")
                .add("uniqueId=" + uniqueId)
                .add("takenAt=" + takenAt)
                .add("payload=" + payload)
                .toString();
    }
}
