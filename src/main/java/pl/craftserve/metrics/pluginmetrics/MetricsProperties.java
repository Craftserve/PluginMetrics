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

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class MetricsProperties extends Properties {
    public enum Keys {
        SAMPLE_INTERVAL(Duration.ofMinutes(1L)),
        REPORT_INTERVAL(Duration.ofMinutes(20L)),
        SERVER_ID(),
        ;

        private final Object defaultValue;

        Keys() {
            this(null);
        }

        Keys(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public final String key() {
            return this.name().toLowerCase(Locale.US);
        }

        public Object getDefaultValue() {
            return Objects.requireNonNull(this.defaultValue);
        }

        public boolean hasDefaultValue() {
            return this.defaultValue != null;
        }

        @Override
        public String toString() {
            return this.key();
        }
    }

    protected MetricsProperties() {
    }

    protected MetricsProperties(Properties defaults) {
        super(defaults);
    }

    public Duration sampleInterval() throws MalformedPropertiesException {
        Duration interval = (Duration) Keys.SAMPLE_INTERVAL.getDefaultValue();

        String value = this.getProperty(Keys.SAMPLE_INTERVAL.key());
        if (value != null) {
            try {
                interval = Duration.ofSeconds(Long.parseLong(value));
            } catch (NumberFormatException e) {
                throw new MalformedPropertiesException(e);
            }
        }

        return interval;
    }

    public MetricsProperties sampleInterval(Duration interval) {
        this.setProperty(Keys.SAMPLE_INTERVAL.key(), interval == null ? null : Long.toString(interval.getSeconds()));
        return this;
    }

    public Duration reportInterval() throws MalformedPropertiesException {
        Duration interval = (Duration) Keys.REPORT_INTERVAL.getDefaultValue();

        String value = this.getProperty(Keys.REPORT_INTERVAL.key());
        if (value != null) {
            try {
                interval = Duration.ofSeconds(Long.parseLong(value));
            } catch (NumberFormatException e) {
                throw new MalformedPropertiesException(e);
            }
        }

        return interval;
    }

    public MetricsProperties reportInterval(Duration interval) {
        this.setProperty(Keys.REPORT_INTERVAL.key(), interval == null ? null : Long.toString(interval.getSeconds()));
        return this;
    }

    public Optional<UUID> serverId() throws MalformedPropertiesException {
        String serverId = this.getProperty(Keys.SERVER_ID.key());
        if (serverId != null) {
            try {
                return Optional.of(UUID.fromString(serverId));
            } catch (IllegalArgumentException e) {
                throw new MalformedPropertiesException(e);
            }
        }

        return Optional.empty();
    }

    public MetricsProperties serverId(UUID serverId) {
        this.setProperty(Keys.SERVER_ID.key(), serverId == null ? null : serverId.toString());
        return this;
    }

    //
    // Factory Methods
    //

    public static MetricsProperties create() {
        return new MetricsProperties();
    }

    public static MetricsProperties create(Properties defaults) {
        return new MetricsProperties(defaults);
    }
}
