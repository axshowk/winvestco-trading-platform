package in.winvestco.marketservice.messaging.mapper;

import com.google.protobuf.Timestamp;
import in.winvestco.common.kafka.market.IndexConstituent;
import in.winvestco.common.kafka.market.IndexSnapshotEvent;
import in.winvestco.common.kafka.market.QuoteUpdatedEvent;
import in.winvestco.marketservice.proto.MarketDataEvent;
import in.winvestco.marketservice.proto.StockData;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class MarketKafkaEventMapper {

    private static final String EVENT_VERSION = "v1";
    private static final String SOURCE = "market-service";

    public QuoteUpdatedEvent toQuoteEvent(MarketDataEvent event) {
        if (event == null) {
            return null;
        }

        return QuoteUpdatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventVersion(EVENT_VERSION)
                .setEventTime(resolveEventTime(event))
                .setSource(SOURCE)
                .setTraceId("")
                .setSymbol(normalizeSymbol(event.getSymbol()))
                .setExchange(event.getExchange())
                .setLastPrice(event.getLastTradedPrice())
                .setOpen(event.getOpen())
                .setHigh(event.getHigh())
                .setLow(event.getLow())
                .setClose(event.getClose())
                .setChange(event.getChangeValue())
                .setChangePercent(event.getChangePercentage())
                .setVolume(event.getVolume())
                .build();
    }

    public QuoteUpdatedEvent toQuoteEvent(StockData stockData) {
        if (stockData == null) {
            return null;
        }

        return QuoteUpdatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventVersion(EVENT_VERSION)
                .setEventTime(nowTimestamp())
                .setSource(SOURCE)
                .setTraceId("")
                .setSymbol(normalizeSymbol(stockData.getSymbol()))
                .setExchange("NSE")
                .setLastPrice(stockData.getLastPrice())
                .setOpen(stockData.getOpen())
                .setHigh(stockData.getHigh())
                .setLow(stockData.getLow())
                .setClose(stockData.getPreviousClose())
                .setChange(stockData.getChange())
                .setChangePercent(stockData.getPercentChange())
                .setVolume(stockData.getVolume())
                .build();
    }

    public IndexSnapshotEvent toIndexSnapshotEvent(MarketDataEvent event) {
        if (event == null) {
            return null;
        }

        IndexSnapshotEvent.Builder builder = IndexSnapshotEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventVersion(EVENT_VERSION)
                .setEventTime(resolveEventTime(event))
                .setSource(SOURCE)
                .setTraceId("")
                .setIndexSymbol(normalizeSymbol(event.getSymbol()))
                .setIndexName(event.getIndexName())
                .setLastPrice(event.getLastTradedPrice())
                .setChange(event.getChangeValue())
                .setChangePercent(event.getChangePercentage())
                .setVolume(event.getVolume());

        List<StockData> constituents = event.getConstituentsList();
        for (StockData stock : constituents) {
            builder.addConstituents(IndexConstituent.newBuilder()
                    .setSymbol(normalizeSymbol(stock.getSymbol()))
                    .setExchange("NSE")
                    .setLastPrice(stock.getLastPrice())
                    .setWeightage(stock.getIndexWeightage())
                    .build());
        }

        return builder.build();
    }

    private Timestamp resolveEventTime(MarketDataEvent event) {
        if (event.hasTimestamp()) {
            return event.getTimestamp();
        }
        return nowTimestamp();
    }

    private Timestamp nowTimestamp() {
        Instant now = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        return symbol.trim().toUpperCase();
    }
}
