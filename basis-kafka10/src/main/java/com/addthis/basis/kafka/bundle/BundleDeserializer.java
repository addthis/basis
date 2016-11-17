package com.addthis.basis.kafka.bundle;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.list.ListBundle;
import com.addthis.bundle.core.list.ListBundleFormat;
import com.addthis.bundle.io.DataChannelCodec;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public class BundleDeserializer implements Deserializer<Bundle> {

    private ListBundleFormat format = new ListBundleFormat();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // not needed
    }

    @Override
    public Bundle deserialize(String topic, byte[] data) {
        return this.fromBytes(data);
    }

    @Override
    public void close() {
        // not needed
    }

    public Bundle fromBytes(byte[] bytes) {
        try {
            return DataChannelCodec.decodeBundle(new ListBundle(format), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
