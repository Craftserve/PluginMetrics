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
import com.google.gson.JsonObject;
import pl.craftserve.metrics.pluginmetrics.sample.Sampler;
import pl.craftserve.metrics.pluginmetrics.util.TrackedBukkitRunnable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Reporter {
    private final Sampler sampler;
    private final Logger logger;
    private final List<Endpoint> endpoints;
    private final ExecutorService executorService;

    private Task task;

    public Reporter(Sampler sampler, Logger logger, List<Endpoint> endpoints, ExecutorService executorService) {
        this.sampler = Objects.requireNonNull(sampler, "sampler");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.endpoints = ImmutableList.copyOf(Objects.requireNonNull(endpoints, "endpoints"));
        this.executorService = Objects.requireNonNull(executorService, "executorService");

        Preconditions.checkArgument(!endpoints.isEmpty(), "endpoints is empty!");
    }

    public void shutdown() {
        try {
            this.executorService.shutdown();
            this.executorService.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.log(Level.SEVERE, "Could not shutdown executor service for plugin metrics reporter.", e);
        }
    }

    public Optional<Task> getTask() {
        return Optional.ofNullable(this.task);
    }

    public void setTask(Task task) {
        if (this.task != null && this.task.isRunning()) {
            throw new IllegalStateException("Reporter task is running!");
        }

        this.task = task;
    }

    public Future<?> report(Report report) {
        Objects.requireNonNull(report, "report");

        JsonObject json = new JsonObject();
        report.serialize(json);

        return this.executorService.submit(() -> {
            this.logger.fine("Submitting queued samples for plugin metrics.");
            for (Endpoint endpoint : this.endpoints) {
                try {
                    // TODO we should clone this JsonObject for each consumer
                    endpoint.consume(json);
                } catch (Throwable throwable) {
                    this.logger.log(Level.SEVERE, "Could not report " + report.getUniqueId() + " to endpoints.", throwable);
                }
            }
        });
    }

    public class Task extends TrackedBukkitRunnable {
        @Override
        public void run() {
            Reporter reporter = Reporter.this;
            reporter.sampler.createReport().ifPresent(reporter::report);
        }
    }
}
