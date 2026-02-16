package in.winvestco.risk_service.service;

import java.util.List;

public interface NewsSourceService {
    List<String> getNewsForSymbol(String symbol);
}
