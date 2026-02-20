package com.github.NFMdev.cdia.processing_service.flink.job;

import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import com.github.NFMdev.cdia.processing_service.flink.deserializer.EventDebeziumDeserializationSchema;
import com.github.NFMdev.cdia.processing_service.flink.function.AnomalyDetectionFunction;
import com.github.NFMdev.cdia.processing_service.flink.model.Event;
import com.github.NFMdev.cdia.processing_service.flink.model.EventAnomaly;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cdc.connectors.base.options.StartupOptions;
import org.apache.flink.cdc.connectors.base.source.jdbc.JdbcIncrementalSource;
import org.apache.flink.cdc.connectors.postgres.source.PostgresSourceBuilder;
import org.apache.flink.cdc.debezium.DebeziumDeserializationSchema;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch8AsyncSink;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch8AsyncSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.http.HttpHost;

import java.time.Duration;

public class AnomalyJob {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        int executionParallelism = Integer.parseInt(System.getenv().getOrDefault("FLINK_JOB_PARALLELISM", "2"));
        long openThreshold = Long.parseLong(System.getenv().getOrDefault("ANOMALY_OPEN_THRESHOLD", "1000"));
        long closeThreshold = Long.parseLong(System.getenv().getOrDefault("ANOMALY_CLOSE_THRESHOLD", "600"));
        long reAlertSeconds = Long.parseLong(System.getenv().getOrDefault("ANOMALY_REALERT_SECONDS", "15"));
        env.setParallelism(executionParallelism);

        // Custom deserializer for CDC events
        DebeziumDeserializationSchema<Event> deserializer = new EventDebeziumDeserializationSchema();

        // CDC source
        JdbcIncrementalSource<Event> postgresSource = PostgresSourceBuilder.PostgresIncrementalSource.<Event>builder()
                .hostname("postgres")
                .port(5432)
                .database("crime_analytics")
                .tableList("public.events")
                .username("admin")
                .password("admin")
                .slotName("flink")
                .decodingPluginName("pgoutput")
                .startupOptions(StartupOptions.latest())
                .deserializer(deserializer)
                .build();

        // CDC stream from Postgres, uses created_at as event time
        DataStream<Event> cdcEventsStream = env.fromSource(
                postgresSource,
                WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofSeconds(3))
                        .withTimestampAssigner((event, l) -> event.getCreatedAt().getTime()),
                "PostgresCDC"
        ).returns(Event.class);

        // Real-time anomaly detection over a rolling 1-minute window
        DataStream<EventAnomaly> anomalies = cdcEventsStream
                .keyBy(Event::getLocation)
                .process(new AnomalyDetectionFunction(
                        openThreshold,
                        closeThreshold,
                        Duration.ofMinutes(1),
                        Duration.ofSeconds(reAlertSeconds)
                ))
                .name("AnomalyDetector")
                .setParallelism(executionParallelism);

        // ES sink
        Elasticsearch8AsyncSink<EventAnomaly> sink = Elasticsearch8AsyncSinkBuilder.<EventAnomaly>builder()
                .setHosts(HttpHost.create("http://cdia-elasticsearch:9200"))
                .setUsername("elastic")
                .setPassword("test")
                .setMaxBatchSize(100)
                .setMaxInFlightRequests(8)
                .setMaxBufferedRequests(2_000)
                .setMaxBatchSizeInBytes(1_048_576L)
                .setMaxTimeInBufferMS(200L)
                .setElementConverter(
                        (anomaly, ctx) -> new IndexOperation.Builder<EventAnomaly>()
                                .id(anomaly.getId().toString())
                                .document(anomaly)
                                .index("event-anomalies")
                                .build()
                ).build();

        anomalies
                .sinkTo(sink)
                .name("EventAnomalyElasticsearchSink")
                .setParallelism(executionParallelism);

        env.execute("Anomaly Detection Job");
    }

}
