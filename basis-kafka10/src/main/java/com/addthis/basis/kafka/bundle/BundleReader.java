package com.addthis.basis.kafka.bundle;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.Bundles;
import com.addthis.bundle.io.DataChannelCodec;
import kafka.common.MessageReader;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class BundleReader implements MessageReader {

    String topic;
    Scanner scanner;

    @Override
    public void init(InputStream inputStream, Properties props) {
        this.topic = props.getProperty("topic");
        this.scanner = new Scanner(inputStream);
    }

    @Override
    public ProducerRecord<byte[], byte[]> readMessage() {
        try {
            String line = this.scanner.nextLine();
            Bundle bundle = Bundles.decode(line);
            return new ProducerRecord<>(topic, DataChannelCodec.encodeBundle(bundle));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        this.scanner.close();
    }
}
