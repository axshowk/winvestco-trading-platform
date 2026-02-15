package in.winvestco.marketservice.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.marketservice.client.NseClient;
import in.winvestco.marketservice.grpc.MarketDataGrpcService;
import in.winvestco.marketservice.messaging.MarketDataPublisher;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataScheduler {

    private final NseClient nseClient;
    private final MarketDataPublisher marketDataPublisher;
    private final in.winvestco.marketservice.service.MarketDataService marketDataService;
    private final MarketDataGrpcService marketDataGrpcService;
    private final ObjectMapper objectMapper;

    /**
     * Fetch and publish market data - triggered via RabbitMQ
     */
    public void fetchAndPublishMarketData() {
        log.info("Scheduled task started: Fetching market data from NSE India");
        try {
            // Fetch full data for all major NSE indices
            List<String> indices = List.of(
                    // Broad Market Indices
                    "NIFTY 50", "NIFTY NEXT 50", "NIFTY 100", "NIFTY 200", "NIFTY 500",
                    "NIFTY MIDCAP 50", "NIFTY MIDCAP 100", "NIFTY SMLCAP 100",
                    // Sectoral Indices
                    "NIFTY BANK", "NIFTY IT", "NIFTY AUTO", "NIFTY FINANCIAL SERVICES",
                    "NIFTY FMCG", "NIFTY PHARMA", "NIFTY METAL", "NIFTY MEDIA",
                    "NIFTY ENERGY", "NIFTY PSU BANK", "NIFTY PRIVATE BANK",
                    "NIFTY INFRA", "NIFTY REALTY", "NIFTY CONSUMPTION");

            for (String indexName : indices) {
                // Get full NSE response with all constituent stocks
                String jsonData = nseClient.getFullIndexData(indexName);
                if (jsonData != null) {
                    marketDataPublisher.publishMarketData(jsonData);
                    marketDataService.saveMarketData(indexName, jsonData);

                    // Parse JSON once, pass parsed tree to gRPC service
                    // Avoids redundant objectMapper.readTree() inside pushUpdatesForIndex
                    try {
                        JsonNode parsedRoot = objectMapper.readTree(jsonData);
                        marketDataGrpcService.pushUpdatesFromParsedIndex(indexName, parsedRoot);
                    } catch (Exception e) {
                        log.warn("Failed to push gRPC updates for index {}: {}", indexName, e.getMessage());
                    }

                    log.info("Successfully fetched and published full data for: {}", indexName);
                } else {
                    log.warn("No data received for index: {}", indexName);
                }
            }

        } catch (Exception e) {
            log.error("Error occurred while fetching/publishing market data from NSE", e);
        }
    }
}
