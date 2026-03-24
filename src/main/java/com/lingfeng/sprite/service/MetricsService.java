package com.lingfeng.sprite.service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

import javax.management.MBeanServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S27-1: Prometheus Metrics Export Service
 *
 * Provides metrics export in Prometheus text format with support for:
 * - Counter: monotonically increasing values
 * - Gauge: current value that can go up or down
 * - Histogram: distribution of values in buckets
 * - Summary: quantiles over a sliding time window
 *
 * Sprite-specific metrics are tracked for:
 * - Cognition cycles
 * - Decisions
 * - Memory operations
 * - Evolution events
 */
@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    // Metric storage
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final Map<String, Histogram> histograms = new ConcurrentHashMap<>();
    private final Map<String, Summary> summaries = new ConcurrentHashMap<>();

    // JVM metrics suppliers
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    // Prefix for all metrics
    private static final String METRIC_PREFIX = "sprite_";

    public MetricsService() {
        initializeDefaultMetrics();
        logger.info("MetricsService initialized with Prometheus export support");
    }

    /**
     * Initialize default JVM and system metrics
     */
    private void initializeDefaultMetrics() {
        // JVM Memory metrics
        registerGauge("jvm_memory_heap_used_bytes", "JVM heap memory used in bytes", () ->
            (double) memoryBean.getHeapMemoryUsage().getUsed());
        registerGauge("jvm_memory_heap_max_bytes", "JVM heap memory max in bytes", () ->
            (double) memoryBean.getHeapMemoryUsage().getMax());
        registerGauge("jvm_memory_heap_used_percent", "JVM heap memory usage percentage", () -> {
            MemoryUsage usage = memoryBean.getHeapMemoryUsage();
            long max = usage.getMax() >= 0 ? usage.getMax() : usage.getCommitted();
            return max > 0 ? (double) usage.getUsed() / max * 100 : 0;
        });

        // Thread metrics
        registerGauge("jvm_threads_total", "Total JVM threads", () ->
            (double) threadBean.getThreadCount());
        registerGauge("jvm_threads_peak", "Peak JVM threads", () ->
            (double) threadBean.getPeakThreadCount());
        registerGauge("jvm_threads_daemon", "Daemon JVM threads", () ->
            (double) threadBean.getDaemonThreadCount());

        // Sprite-specific metrics
        registerCounter("sprite_cognition_cycles_total", "Total cognition cycles executed");
        registerCounter("sprite_decisions_total", "Total decisions made");
        registerCounter("sprite_memories_stored_total", "Total memories stored");
        registerCounter("sprite_memories_retrieved_total", "Total memories retrieved");
        registerCounter("sprite_evolution_events_total", "Total evolution events triggered");
        registerCounter("sprite_actions_executed_total", "Total actions executed");
        registerCounter("sprite_actions_success_total", "Total successful actions");
        registerCounter("sprite_actions_failed_total", "Total failed actions");

        // Latency histograms
        registerHistogram("sprite_cognition_duration_seconds", "Cognition cycle duration in seconds",
            Arrays.asList(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0));
        registerHistogram("sprite_action_duration_seconds", "Action execution duration in seconds",
            Arrays.asList(0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0));

        // Summary for end-to-end latency
        registerSummary("sprite_request_duration_seconds", "Request duration summary",
            Arrays.asList(0.5, 0.9, 0.95, 0.99));
    }

    // ==================== Counter Operations ====================

    /**
     * S27-1: Increase a counter by name with optional labels
     */
    public void incrementCounter(String name, String... labels) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        Counter counter = counters.computeIfAbsent(fullName, k -> new Counter(fullName, "", labels));
        counter.inc();
        logger.debug("Incremented counter: {} {}", fullName, Arrays.toString(labels));
    }

    /**
     * S27-1: Increase a counter by a specific delta with optional labels
     */
    public void incrementCounterBy(String name, double delta, String... labels) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        Counter counter = counters.computeIfAbsent(fullName, k -> new Counter(fullName, "", labels));
        counter.inc(delta);
    }

    /**
     * S27-1: Register a counter with a description (for initialization)
     */
    public void registerCounter(String name, String description) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        counters.computeIfAbsent(fullName, k -> new Counter(fullName, description, new String[0]));
        logger.debug("Registered counter: {} ({})", fullName, description);
    }

    // ==================== Gauge Operations ====================

    /**
     * S27-1: Set a gauge value with optional labels
     */
    public void setGauge(String name, double value, String... labels) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        Gauge gauge = gauges.computeIfAbsent(fullName, k -> new Gauge(fullName, "", labels));
        gauge.set(value);
        logger.debug("Set gauge: {} = {} {}", fullName, value, Arrays.toString(labels));
    }

    /**
     * Register a gauge with a dynamic value supplier
     */
    public void registerGauge(String name, String description, DoubleSupplier valueSupplier) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        gauges.computeIfAbsent(fullName, k -> new Gauge(fullName, description, new String[0], valueSupplier));
    }

    // ==================== Histogram Operations ====================

    /**
     * S27-1: Record a histogram observation with optional labels
     */
    public void observeHistogram(String name, double value, String... labels) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        Histogram histogram = histograms.computeIfAbsent(fullName, k -> {
            List<Double> defaultBuckets = Arrays.asList(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0);
            return new Histogram(fullName, "", labels, defaultBuckets);
        });
        histogram.observe(value);
        logger.debug("Observed histogram: {} = {} {}", fullName, value, Arrays.toString(labels));
    }

    /**
     * Register a histogram with custom buckets
     */
    public void registerHistogram(String name, String description, List<Double> buckets) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        histograms.computeIfAbsent(fullName, k -> new Histogram(fullName, description, new String[0], buckets));
    }

    // ==================== Summary Operations ====================

    /**
     * Record a summary observation with optional labels
     */
    public void observeSummary(String name, double value, String... labels) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        Summary summary = summaries.computeIfAbsent(fullName, k -> {
            List<Double> defaultQuantiles = Arrays.asList(0.5, 0.9, 0.95, 0.99);
            return new Summary(fullName, "", labels, defaultQuantiles);
        });
        summary.observe(value);
    }

    /**
     * Register a summary with custom quantiles
     */
    public void registerSummary(String name, String description, List<Double> quantiles) {
        String fullName = METRIC_PREFIX + sanitizeName(name);
        summaries.computeIfAbsent(fullName, k -> new Summary(fullName, description, new String[0], quantiles));
    }

    // ==================== Prometheus Export ====================

    /**
     * S27-1: Get all metrics in Prometheus text format
     */
    public String getPrometheusMetrics() {
        StringBuilder sb = new StringBuilder();

        // Add help and type info as comments
        sb.append("# Prometheus metrics exported by Sprite\n");
        sb.append("# Generated at: ").append(Instant.now()).append("\n\n");

        // Export counters
        for (Counter counter : counters.values()) {
            sb.append(counter.toPrometheusFormat());
            sb.append("\n");
        }

        // Export gauges
        for (Gauge gauge : gauges.values()) {
            sb.append(gauge.toPrometheusFormat());
            sb.append("\n");
        }

        // Export histograms
        for (Histogram histogram : histograms.values()) {
            sb.append(histogram.toPrometheusFormat());
            sb.append("\n");
        }

        // Export summaries
        for (Summary summary : summaries.values()) {
            sb.append(summary.toPrometheusFormat());
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Get metrics as map (for internal use)
     */
    public Map<String, Double> getMetricsSnapshot() {
        Map<String, Double> snapshot = new ConcurrentHashMap<>();

        counters.forEach((name, counter) -> snapshot.put(name, (double) counter.getValue()));
        gauges.forEach((name, gauge) -> snapshot.put(name, gauge.getValue()));
        histograms.forEach((name, histogram) -> snapshot.put(name + "_count", (double) histogram.getCount()));
        summaries.forEach((name, summary) -> snapshot.put(name + "_count", (double) summary.getCount()));

        return snapshot;
    }

    // ==================== Sprite-Specific Convenience Methods ====================

    /**
     * Record a cognition cycle completion with duration
     */
    public void recordCognitionCycle(double durationSeconds) {
        incrementCounter("cognition_cycles_total");
        observeHistogram("cognition_duration_seconds", durationSeconds);
    }

    /**
     * Record a decision made
     */
    public void recordDecision() {
        incrementCounter("decisions_total");
    }

    /**
     * Record memory stored
     */
    public void recordMemoryStored() {
        incrementCounter("memories_stored_total");
    }

    /**
     * Record memory retrieved
     */
    public void recordMemoryRetrieved() {
        incrementCounter("memories_retrieved_total");
    }

    /**
     * Record evolution event
     */
    public void recordEvolutionEvent() {
        incrementCounter("evolution_events_total");
    }

    /**
     * Record action execution with success/failure
     */
    public void recordActionExecuted(boolean success, double durationSeconds) {
        incrementCounter("actions_executed_total");
        if (success) {
            incrementCounter("actions_success_total");
        } else {
            incrementCounter("actions_failed_total");
        }
        observeHistogram("action_duration_seconds", durationSeconds);
    }

    // ==================== Helper Classes ====================

    /**
     * Counter metric - monotonically increasing value
     */
    public static class Counter {
        private final String name;
        private final String description;
        private final String[] labelKeys;
        private final AtomicLong value = new AtomicLong(0);

        public Counter(String name, String description, String[] labelKeys) {
            this.name = name;
            this.description = description;
            this.labelKeys = labelKeys;
        }

        public void inc() {
            value.incrementAndGet();
        }

        public void inc(double delta) {
            value.addAndGet((long) delta);
        }

        public long getValue() {
            return value.get();
        }

        public String toPrometheusFormat() {
            StringBuilder sb = new StringBuilder();
            sb.append("# HELP ").append(name).append(" ").append(description).append("\n");
            sb.append("# TYPE ").append(name).append(" counter\n");
            sb.append(name);
            if (labelKeys.length > 0) {
                sb.append("{");
                for (int i = 0; i < labelKeys.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(labelKeys[i]).append("=\"unknown\"");
                }
                sb.append("}");
            }
            sb.append(" ").append(value.get());
            return sb.toString();
        }
    }

    /**
     * Gauge metric - current value that can go up or down
     */
    public static class Gauge {
        private final String name;
        private final String description;
        private final String[] labelKeys;
        private final DoubleSupplier supplier;
        private volatile double value = 0;
        private volatile boolean hasSupplier = false;

        public Gauge(String name, String description, String[] labelKeys) {
            this(name, description, labelKeys, () -> 0.0);
        }

        public Gauge(String name, String description, String[] labelKeys, DoubleSupplier supplier) {
            this.name = name;
            this.description = description;
            this.labelKeys = labelKeys;
            this.supplier = supplier;
            this.hasSupplier = true;
        }

        public void set(double value) {
            this.value = value;
            this.hasSupplier = false;
        }

        public double getValue() {
            return hasSupplier ? supplier.getAsDouble() : value;
        }

        public String toPrometheusFormat() {
            StringBuilder sb = new StringBuilder();
            sb.append("# HELP ").append(name).append(" ").append(description).append("\n");
            sb.append("# TYPE ").append(name).append(" gauge\n");
            sb.append(name);
            if (labelKeys.length > 0) {
                sb.append("{");
                for (int i = 0; i < labelKeys.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(labelKeys[i]).append("=\"unknown\"");
                }
                sb.append("}");
            }
            sb.append(" ").append(getValue());
            return sb.toString();
        }
    }

    /**
     * Histogram metric - distribution of values in buckets
     */
    public static class Histogram {
        private final String name;
        private final String description;
        private final String[] labelKeys;
        private final List<Double> buckets;
        private final long[] bucketCounts;
        private final AtomicLong count = new AtomicLong(0);
        private final ConcurrentLinkedDeque<Double> values = new ConcurrentLinkedDeque<>();
        private double sum = 0;

        public Histogram(String name, String description, String[] labelKeys, List<Double> buckets) {
            this.name = name;
            this.description = description;
            this.labelKeys = labelKeys;
            this.buckets = new ArrayList<>(buckets);
            this.bucketCounts = new long[this.buckets.size() + 1]; // +1 for +Inf bucket
            // Sort buckets for proper cumulative counting
            this.buckets.sort(Double::compareTo);
        }

        public void observe(double value) {
            count.incrementAndGet();
            sum += value;
            values.addLast(value);

            // Update buckets
            for (int i = 0; i < buckets.size(); i++) {
                if (value <= buckets.get(i)) {
                    bucketCounts[i]++;
                }
            }
            bucketCounts[bucketCounts.length - 1]++; // +Inf bucket always increments

            // Keep only last 10000 values to prevent memory issues
            while (values.size() > 10000) {
                Double removed = values.removeFirst();
                sum -= removed;
            }
        }

        public long getCount() {
            return count.get();
        }

        public double getSum() {
            return sum;
        }

        public String toPrometheusFormat() {
            StringBuilder sb = new StringBuilder();
            sb.append("# HELP ").append(name).append(" ").append(description).append("\n");
            sb.append("# TYPE ").append(name).append(" histogram\n");

            // Bucket metrics
            double cumulativeCount = 0;
            for (int i = 0; i < buckets.size(); i++) {
                cumulativeCount += bucketCounts[i];
                sb.append(name).append("_bucket{le=\"").append(buckets.get(i)).append("\"} ")
                    .append(cumulativeCount).append("\n");
            }
            // +Inf bucket
            sb.append(name).append("_bucket{le=\"+Inf\"} ").append(count.get()).append("\n");

            // Count and sum
            sb.append(name).append("_count ").append(count.get()).append("\n");
            sb.append(name).append("_sum ").append(sum).append("\n");

            return sb.toString();
        }
    }

    /**
     * Summary metric - quantiles over a sliding time window
     */
    public static class Summary {
        private final String name;
        private final String description;
        private final String[] labelKeys;
        private final List<Double> quantiles;
        private final ConcurrentLinkedDeque<Double> values = new ConcurrentLinkedDeque<>();
        private final AtomicLong count = new AtomicLong(0);
        private double sum = 0;

        public Summary(String name, String description, String[] labelKeys, List<Double> quantiles) {
            this.name = name;
            this.description = description;
            this.labelKeys = labelKeys;
            this.quantiles = new ArrayList<>(quantiles);
            this.quantiles.sort(Double::compareTo);
        }

        public void observe(double value) {
            count.incrementAndGet();
            sum += value;
            values.addLast(value);

            // Keep only last 10000 values
            while (values.size() > 10000) {
                Double removed = values.removeFirst();
                sum -= removed;
            }
        }

        public long getCount() {
            return count.get();
        }

        public double getSum() {
            return sum;
        }

        public String toPrometheusFormat() {
            StringBuilder sb = new StringBuilder();
            sb.append("# HELP ").append(name).append(" ").append(description).append("\n");
            sb.append("# TYPE ").append(name).append(" summary\n");

            // Calculate quantiles from sorted values
            List<Double> sortedValues = new ArrayList<>(values);
            sortedValues.sort(Double::compareTo);

            for (double quantile : quantiles) {
                double value = calculateQuantile(sortedValues, quantile);
                sb.append(name).append("{quantile=\"").append(quantile).append("\"} ")
                    .append(value).append("\n");
            }

            // Count and sum
            sb.append(name).append("_count ").append(count.get()).append("\n");
            sb.append(name).append("_sum ").append(sum).append("\n");

            return sb.toString();
        }

        private double calculateQuantile(List<Double> sortedValues, double quantile) {
            if (sortedValues.isEmpty()) {
                return 0.0;
            }
            int index = (int) Math.ceil(quantile * sortedValues.size()) - 1;
            index = Math.max(0, Math.min(index, sortedValues.size() - 1));
            return sortedValues.get(index);
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Sanitize metric name to follow Prometheus naming conventions
     */
    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_")
                   .replaceAll("^[^a-zA-Z]", "m_$0")
                   .toLowerCase();
    }

    /**
     * Clear all metrics (for testing)
     */
    public void clear() {
        counters.clear();
        gauges.clear();
        histograms.clear();
        summaries.clear();
        initializeDefaultMetrics();
        logger.info("All metrics cleared and reinitialized");
    }

    /**
     * Get metric statistics summary
     */
    public String getMetricsSummary() {
        return String.format(
            "Metrics Summary - Counters: %d, Gauges: %d, Histograms: %d, Summaries: %d",
            counters.size(), gauges.size(), histograms.size(), summaries.size()
        );
    }
}
