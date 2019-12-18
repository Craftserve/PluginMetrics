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

package pl.craftserve.metrics.pluginmetrics.report;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pl.craftserve.metrics.pluginmetrics.sample.Sample;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

public class Report {
    private final UUID uniqueId;
    private final List<Sample> samples;

    public Report(UUID uniqueId, List<Sample> samples) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.samples = ImmutableList.copyOf(Objects.requireNonNull(samples, "samples"));

        Preconditions.checkArgument(!samples.isEmpty(), "samples is empty!");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report report = (Report) o;
        return Objects.equals(uniqueId, report.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public List<Sample> getSamples() {
        return this.samples;
    }

    public void serialize(JsonObject object) {
        Objects.requireNonNull(object, "object");
        object.addProperty("id", this.uniqueId.toString());

        JsonArray samples = new JsonArray();
        for (Sample sample : this.samples) {
            JsonObject sampleObject = new JsonObject();
            sample.serialize(sampleObject);
            samples.add(sampleObject);
        }

        object.add("samples", samples);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Report.class.getSimpleName() + "[", "]")
                .add("uniqueId=" + uniqueId)
                .add("samples=" + samples)
                .toString();
    }
}
