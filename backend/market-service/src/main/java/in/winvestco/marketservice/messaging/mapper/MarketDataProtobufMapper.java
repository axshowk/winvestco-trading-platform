package in.winvestco.marketservice.messaging.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Timestamp;
import in.winvestco.marketservice.dto.MarketDataDTO;
import in.winvestco.marketservice.proto.MarketDataEvent;
import in.winvestco.marketservice.proto.StockData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting between MarketDataDTO and Protobuf MarketDataEvent.
 */
@Component
@Slf4j
public class MarketDataProtobufMapper {

    /**
     * Convert MarketDataDTO to Protobuf MarketDataEvent.
     */
    public MarketDataEvent toProtobuf(MarketDataDTO dto) {
        if (dto == null) {
            return null;
        }

        MarketDataEvent.Builder builder = MarketDataEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setSymbol(dto.getSymbol() != null ? dto.getSymbol() : "")
                .setExchange(dto.getExchange() != null ? dto.getExchange() : "NSE")
                .setTradingSymbol(dto.getTradingSymbol() != null ? dto.getTradingSymbol() : "")
                .setLastTradedPrice(toDouble(dto.getLastTradedPrice()))
                .setOpen(toDouble(dto.getOpen()))
                .setHigh(toDouble(dto.getHigh()))
                .setLow(toDouble(dto.getLow()))
                .setClose(toDouble(dto.getClose()))
                .setChangeValue(toDouble(dto.getChangeValue()))
                .setChangePercentage(toDouble(dto.getChangePercentage()))
                .setVolume(toLong(dto.getVolume()))
                .setTotalBuyQuantity(toLong(dto.getTotalBuyQuantity()))
                .setTotalSellQuantity(toLong(dto.getTotalSellQuantity()))
                .setUpperCircuitLimit(toDouble(dto.getUpperCircuitLimit()))
                .setLowerCircuitLimit(toDouble(dto.getLowerCircuitLimit()))
                .setDataSource("NSE");

        // Set timestamp
        if (dto.getTimestamp() != null) {
            Instant instant = dto.getTimestamp().atZone(ZoneId.systemDefault()).toInstant();
            builder.setTimestamp(Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build());
        } else {
            Instant now = Instant.now();
            builder.setTimestamp(Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build());
        }

        return builder.build();
    }

    /**
     * Parse full NSE index JSON response and create MarketDataEvent with constituents.
     */
    public MarketDataEvent fromNseJson(String indexName, JsonNode root) {
        if (root == null) {
            return null;
        }

        MarketDataEvent.Builder builder = MarketDataEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setIndexName(indexName != null ? indexName : "")
                .setExchange("NSE")
                .setDataSource("NSE");

        // Parse timestamp from metadata if available
        JsonNode metadata = root.path("metadata");
        if (!metadata.isMissingNode()) {
            String timestampStr = metadata.path("timestamp").asText();
            builder.setTimestamp(Timestamp.newBuilder()
                    .setSeconds(System.currentTimeMillis() / 1000)
                    .build());
        } else {
            Instant now = Instant.now();
            builder.setTimestamp(Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .build());
        }

        // Parse index-level data from first element if available
        JsonNode dataArray = root.path("data");
        if (dataArray.isArray() && dataArray.size() > 0) {
            JsonNode indexData = dataArray.get(0);
            builder.setSymbol(getText(indexData, "symbol"))
                    .setTradingSymbol(indexName != null ? indexName.replace(" ", "-") : "")
                    .setLastTradedPrice(getDouble(indexData, "lastPrice"))
                    .setOpen(getDouble(indexData, "open"))
                    .setHigh(getDouble(indexData, "dayHigh"))
                    .setLow(getDouble(indexData, "dayLow"))
                    .setClose(getDouble(indexData, "previousClose"))
                    .setChangeValue(getDouble(indexData, "change"))
                    .setChangePercentage(getDouble(indexData, "pChange"))
                    .setVolume(getLong(indexData, "totalTradedVolume"));
        }

        // Parse constituent stocks
        List<StockData> constituents = new ArrayList<>();
        if (dataArray.isArray()) {
            for (JsonNode stockNode : dataArray) {
                StockData stock = parseStockData(stockNode);
                if (stock != null) {
                    constituents.add(stock);
                }
            }
        }
        builder.addAllConstituents(constituents);

        return builder.build();
    }

    private StockData parseStockData(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }

        return StockData.newBuilder()
                .setSymbol(getText(node, "symbol"))
                .setTradingSymbol(getText(node, "symbol"))
                .setCompanyName(getText(node, "meta").isEmpty() ? getText(node, "symbol") : getText(node, "meta"))
                .setSeries(getText(node, "series"))
                .setLastPrice(getDouble(node, "lastPrice"))
                .setOpen(getDouble(node, "open"))
                .setHigh(getDouble(node, "dayHigh"))
                .setLow(getDouble(node, "dayLow"))
                .setPreviousClose(getDouble(node, "previousClose"))
                .setChange(getDouble(node, "change"))
                .setPercentChange(getDouble(node, "pChange"))
                .setVolume(getLong(node, "totalTradedVolume"))
                .setValue(getDouble(node, "totalTradedValue"))
                .setYearHigh(getDouble(node, "yearHigh"))
                .setYearLow(getDouble(node, "yearLow"))
                .setIndexWeightage(getDouble(node, "weight"))
                .build();
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private double getDouble(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isMissingNode() && !value.isNull()) {
            try {
                return value.asDouble(0.0);
            } catch (Exception e) {
                log.debug("Could not parse {} as double", field);
            }
        }
        return 0.0;
    }

    private long getLong(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isMissingNode() && !value.isNull()) {
            try {
                return value.asLong(0);
            } catch (Exception e) {
                log.debug("Could not parse {} as long", field);
            }
        }
        return 0L;
    }

    private double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private long toLong(Long value) {
        return value != null ? value : 0L;
    }
}
