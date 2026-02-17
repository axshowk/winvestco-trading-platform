package in.winvestco.marketservice.messaging.serialization;

import com.google.protobuf.Message;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka Serializer for Protobuf messages.
 * Generic serializer that works with any generated Protobuf message.
 */
public class ProtobufSerializer<T extends Message> implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        return data.toByteArray();
    }
}
