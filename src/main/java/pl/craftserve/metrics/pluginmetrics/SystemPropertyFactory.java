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

import java.util.Objects;

public class SystemPropertyFactory implements RecordFactory<String> {
    protected final String key;

    protected SystemPropertyFactory(String key) {
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public String get(Metrics metrics) throws Throwable {
        return System.getProperty(this.key);
    }

    public static SystemPropertyFactory create(String key) {
        Objects.requireNonNull(key, "key");
        return new SystemPropertyFactory(key);
    }
}
