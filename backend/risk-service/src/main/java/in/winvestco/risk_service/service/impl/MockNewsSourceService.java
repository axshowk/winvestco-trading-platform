package in.winvestco.risk_service.service.impl;

import in.winvestco.risk_service.service.NewsSourceService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class MockNewsSourceService implements NewsSourceService {

    @Override
    public List<String> getNewsForSymbol(String symbol) {
        // Mock data logic
        if ("SCANDAL_INC".equalsIgnoreCase(symbol)) {
            return Arrays.asList(
                "CEO arrested for massive fraud scheme involving offshore accounts.",
                "Auditors resign citing 'irregularities' in financial statements.",
                "Stock plummets 40% in pre-market trading amid scandal."
            );
        } else if ("RISKY_BIOTECH".equalsIgnoreCase(symbol)) {
             return Arrays.asList(
                "New drug trial shows checks and balances failure.",
                "FDA puts clinical hold on key pipeline asset due to safety concerns.",
                "Class action lawsuit filed by investors."
            );
        } else if ("SAFE_CORP".equalsIgnoreCase(symbol)) {
            return Arrays.asList(
                "Quarterly earnings beat expectations by 15%.",
                "Company announces dividend hike and share buyback program.",
                "New product line receives rave reviews from critics."
            );
        }
        
        return Collections.singletonList("No significant recent news found for " + symbol);
    }
}
