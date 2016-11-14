package com.addthis.basis.kafka;

import javax.validation.constraints.NotNull;

import java.util.Map;

import com.codahale.metrics.MetricRegistry;

import kafka.serializer.Decoder;

/**
 * Consumer configuration helper for config files and building a kafka consumer service. This should
 * be used for building consumers in dropwizard apps for example.
 */
public class KafkaConsumerFactory {

    @NotNull
    private String groupID;

    @NotNull
    private String zookeeper;

    @NotNull
    private Map<String, Integer> topics;


    private Map<String, String> overrides;

    public KafkaConsumerFactory() {
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public String getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(String zookeeper) {
        this.zookeeper = zookeeper;
    }

    public Map<String, Integer> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, Integer> topics) {
        this.topics = topics;
    }

    public Map<String, String> getOverrides() {
        return overrides;
    }

    public void setOverrides(Map<String, String> overrides) {
        this.overrides = overrides;
    }

    public <K, V> KafkaConsumerService<K, V> build(MetricRegistry metrics,
                                                   Decoder<K> keyDecoder,
                                                   Decoder<V> valueDecoder,
                                                   MessageHandler<K, V> handler) {
        return KafkaConsumerService.<K, V>newBuilder()
                .metrics(metrics)
                .groupID(groupID)
                .zookeeper(zookeeper)
                .handler(handler)
                .valueDecoder(valueDecoder)
                .keyDecoder(keyDecoder)
                .topics(topics)
                .overrides(overrides)
                .build();
    }
}
