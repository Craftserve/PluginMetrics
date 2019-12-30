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

import com.google.common.io.Closer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public class ServerIdResolver {
    private static final File FILE = new File("csrv-plugin-metrics.properties");

    private final File file;
    protected UUID serverId;

    public ServerIdResolver(File file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public UUID getId() throws IOException {
        if (this.serverId == null) {
            this.serverId = this.resolveServerId();
        }

        return Objects.requireNonNull(this.serverId);
    }

    protected UUID resolveServerId() throws IOException {
        Properties properties = new Properties();
        String key = "server_id";

        if (this.file.exists()) {
            this.read(properties);

            String serverIdString = properties.getProperty(key);
            if (serverIdString != null) {
                try {
                    return UUID.fromString(serverIdString);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        UUID serverId = UUID.randomUUID();
        properties.setProperty(key, serverId.toString());

        this.write(properties);
        return serverId;
    }

    private void read(Properties properties) throws IOException {
        Objects.requireNonNull(properties, "properties");

        try (Closer closer = Closer.create()) {
            FileInputStream fileInputStream = closer.register(new FileInputStream(this.file));
            BufferedInputStream bufferedInputStream = closer.register(new BufferedInputStream(fileInputStream));
            properties.load(bufferedInputStream);
        }
    }

    private void write(Properties properties) throws IOException {
        Objects.requireNonNull(properties, "properties");

        try (Closer closer = Closer.create()) {
            FileOutputStream fileOutputStream = closer.register(new FileOutputStream(this.file));
            BufferedOutputStream bufferedOutputStream = closer.register(new BufferedOutputStream(fileOutputStream));
            properties.store(bufferedOutputStream, null);
        }
    }

    public static ServerIdResolver create() {
        return new ServerIdResolver(FILE);
    }
}
