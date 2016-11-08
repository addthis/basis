/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.basis.kafka8;

import java.io.Closeable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;

import static com.addthis.hydra.kafka.consumer.ConsumerUtils.newConsumerConfig;
import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class KafkaConsumerService<K, V> implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final MetricRegistry metrics;
    private final ConsumerConfig consumerConfig;
    private final Map<String, Integer> topics;
    private final Decoder<K> keyDecoder;
    private final Decoder<V> valueDecoder;
    private final MessageHandler<K, V> messageHandler;

    private ConsumerConnector consumer;
    private ExecutorService streamExecutorService;

    KafkaConsumerService(MetricRegistry metrics,
                         ConsumerConfig consumerConfig,
                         Map<String, Integer> topics,
                         Decoder<K> keyDecoder,
                         Decoder<V> valueDecoder,
                         MessageHandler<K, V> messageHandler) {

        MDC.put("group_id", consumerConfig.groupId());

        this.metrics = metrics;
        this.consumerConfig = consumerConfig;
        this.topics = topics;
        this.keyDecoder = keyDecoder;
        this.valueDecoder = valueDecoder;
        this.messageHandler = messageHandler;
    }

    /**
     * Starts the kafka consumer and streams for each topic. This method will block
     * until the consumer connect is able to connect to kafka / applicable topics.
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("starting kafka consumer");

            int streamCount = topics.values().stream().mapToInt(t -> t).sum();

            ExecutorService streamExecutor = newFixedThreadPool(streamCount, new ThreadFactoryBuilder()
                    .setNameFormat("KafkaStreamConsumer-%d")
                    .setDaemon(true)
                    .build());

            streamExecutorService = new InstrumentedExecutorService(streamExecutor, metrics, name(KafkaConsumerService.class, "streams"));

            consumer = Consumer.createJavaConsumerConnector(consumerConfig);

            consumer.createMessageStreams(topics, keyDecoder, valueDecoder)
                    .forEach((topic, streams) -> {

                        String baseName = name(KafkaConsumerService.class, topic);
                        final Timer messageTimer = metrics.timer(name(baseName, "message_timer"));
                        final Meter errorMeter = metrics.meter(name(baseName, "error_meter"));

                        for (KafkaStream<K, V> stream : streams) {
                            streamExecutorService.execute(() -> {
                                for (MessageAndMetadata<K, V> event : stream) {
                                    try (Timer.Context context = messageTimer.time()) {
                                        messageHandler.handle(event.topic(), event.key(), event.message());
                                    } catch (Exception ex) {
                                        errorMeter.mark();
                                        logger.error("unchecked stream handler exception", ex);
                                    }
                                }
                            });
                        }
                    });

            logger.info("started kafka consumer");
        }
    }


    public void close() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("shutting down kafka consumer");
            if (consumer != null) {
                consumer.shutdown();
            }
            MoreExecutors.shutdownAndAwaitTermination(streamExecutorService, 5, TimeUnit.SECONDS);
            this.messageHandler.close();
            logger.info("shut down kafka consumer");
        }
    }

    public static <K, V> Builder<K, V> newBuilder() {
        return new Builder<>();
    }

    public static class Builder<K, V> {

        private Map<String, Integer> topics = new HashMap<>();
        private Map<String, String> overrides = new HashMap<>();

        private String zookeeper;
        private String groupID;
        private Decoder<K> keyDecoder;
        private Decoder<V> valueDecoder;
        private MessageHandler<K, V> messageHandler;
        private MetricRegistry metrics;

        public Builder<K, V> groupID(String groupID) {
            this.groupID = groupID;
            return this;
        }

        public Builder<K, V> metrics(MetricRegistry metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder<K, V> zookeeper(String zookeeper) {
            this.zookeeper = zookeeper;
            return this;
        }

        public Builder<K, V> addTopic(String topic, int streamCount) {
            this.topics.put(topic, streamCount);
            return this;
        }

        public Builder<K, V> topics(Map<String, Integer> topics) {
            this.topics = topics;
            return this;
        }

        public Builder<K, V> addOverride(String key, String value) {
            this.overrides.put(key, value);
            return this;
        }

        public Builder<K, V> overrides(Map<String, String> overrides) {
            this.overrides = overrides;
            return this;
        }

        public Builder<K, V> keyDecoder(Decoder<K> keyDecoder) {
            this.keyDecoder = keyDecoder;
            return this;
        }

        public Builder<K, V> valueDecoder(Decoder<V> valueDecoder) {
            this.valueDecoder = valueDecoder;
            return this;
        }

        public Builder<K, V> handler(MessageHandler<K, V> messageHandler) {
            this.messageHandler = messageHandler;
            return this;
        }

        public KafkaConsumerService<K, V> build() {
            checkNotNull(groupID, "groupID cannot be null");
            checkNotNull(zookeeper, "zookeeper cannot be null");
            checkNotNull(messageHandler, "handler cannot be null");
            checkNotNull(keyDecoder, "keyDecoder cannot be null");
            checkNotNull(valueDecoder, "valueDecoder cannot be null");
            checkArgument(topics != null && topics.size() > 0, "topics must have at least one topic");

            topics.forEach((topic, streamCount) -> {
                checkArgument(streamCount > 0, format("stream count for topic %s must be greater than 0", topic));
            });

            if (overrides == null) {
                overrides = new HashMap<>();
            }

            overrides.put("group.id", groupID);

            return new KafkaConsumerService<>(metrics, newConsumerConfig(zookeeper, overrides), topics, keyDecoder, valueDecoder, messageHandler);
        }
    }
}