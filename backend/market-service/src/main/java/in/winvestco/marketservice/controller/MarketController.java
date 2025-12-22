package in.winvestco.marketservice.controller;

import in.winvestco.marketservice.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketDataService marketDataService;

    @GetMapping("/indices/{symbol}")
    public ResponseEntity<String> getIndexData(@PathVariable String symbol) {
        String data = marketDataService.getMarketData(symbol);
        if (data != null) {
            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stocks/all")
    public ResponseEntity<String> getAllStocks() {
        String data = marketDataService.getAllStocks();
        if (data != null) {
            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/stocks/{symbol}")
    public ResponseEntity<String> getStockQuote(@PathVariable String symbol) {
        String data = marketDataService.getStockQuote(symbol);
        if (data != null) {
            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
