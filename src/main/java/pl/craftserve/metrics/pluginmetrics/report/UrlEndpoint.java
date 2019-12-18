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

import com.google.gson.JsonObject;
import pl.craftserve.metrics.pluginmetrics.Metrics;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

public class UrlEndpoint implements Endpoint {
    public static final int PROTOCOL_REVISION = 0;
    public static final URL CRAFTSERVE_METRICS;

    static {
        try {
            CRAFTSERVE_METRICS = new URL("https://metrics.craftserve.pl");
        } catch (MalformedURLException e) {
            throw new Error(e); // should never happen
        }
    }

    private final URL url;

    public UrlEndpoint(URL url) {
        this.url = Objects.requireNonNull(url, "url");
    }

    public URL getUrl() {
        return this.url;
    }

    @Override
    public void consume(JsonObject json) throws Throwable {
        URLConnection urlConnection = this.url.openConnection();
        if (!(urlConnection instanceof HttpsURLConnection)) {
            throw new ConnectException("Connection is not an instance of " + HttpsURLConnection.class.getName() + ".");
        }

        HttpsURLConnection connection = (HttpsURLConnection) urlConnection;
        connection.setDoOutput(true);
        connection.addRequestProperty("User-Agent", this.formatUserAgent());
        connection.setRequestMethod("POST");

        // TODO submit JSON object

        do {
            try {
                connection.connect();
                break;
            } catch (SocketTimeoutException ignored) {
                // try again
            }
        } while (true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
            throw new IOException("Request returned " + responseCode + ", " + HttpURLConnection.HTTP_NO_CONTENT + " was expected.");
        }

        connection.disconnect();
    }

    private String formatUserAgent() {
        return Metrics.class.getSimpleName() + "/" + PROTOCOL_REVISION;
    }
}
