package com.addthis.basis.kafka.bundle;

import com.addthis.bundle.core.Bundle;
import kafka.common.MessageFormatter;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public class BundleFormatter implements MessageFormatter{

    private BundleDeserializer deserializer = new BundleDeserializer();

    @Override
    public void init(Properties props) {
        // do nothing
    }

    @Override
    public void writeTo(ConsumerRecord<byte[], byte[]> consumerRecord, PrintStream output) {
        Bundle bundle = this.deserializer.deserialize(null, consumerRecord.value());
        try {
            output.write(bundle.toString().getBytes());
            output.write("\n".getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        // not needed
    }
}
