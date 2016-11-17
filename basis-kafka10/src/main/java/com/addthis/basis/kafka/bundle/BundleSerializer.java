package com.addthis.basis.kafka.bundle;


import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.io.DataChannelCodec;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;


public class BundleSerializer implements Serializer<Bundle> {

    @Override
    public void configure(Map<String, ?> configs, boolean b) {
        // not needed
    }

    @Override
    public byte[] serialize(String topic, Bundle bundle) {
        if(bundle == null) {
            return null;
        }
        try {
            return DataChannelCodec.encodeBundle(bundle);
        } catch (IOException e) {
            //this exception is never actually thrown
            return null;
        }
    }

    @Override
    public void close() {
        // not needed
    }
}
