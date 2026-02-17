package in.winvestco.marketservice.messaging.serialization;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Kafka Deserializer for Protobuf messages.
 * Generic deserializer that works with any generated Protobuf message.
 */
@Slf4j
public class ProtobufDeserializer<T extends Message> implements Deserializer<T> {

    private Parser<T> parser;

    public ProtobufDeserializer() {
    }

    public ProtobufDeserializer(Parser<T> parser) {
        this.parser = parser;
    }

    public void setParser(Parser<T> parser) {
        this.parser = parser;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null || parser == null) {
            return null;
        }
        try {
            return parser.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to deserialize protobuf message from topic: {}", topic, e);
            throw new RuntimeException("Failed to deserialize protobuf message", e);
        }
    }
}
