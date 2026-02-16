package in.winvestco.risk_service.agent;

import in.winvestco.risk_service.dto.RiskEvaluationResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface NewsRiskAgent {

    @SystemMessage("""
            You are a senior financial risk analyst. 
            Given the following news headlines for a company, determine if there is catastrophic risk.
            
            Analyze the sentiment and severity of the news.
            
            Respond with ONLY a raw JSON object. Do NOT wrap the response in markdown code blocks (like ```json).
            
            The JSON object must contain:
            - symbol: the stock symbol
            - riskLevel: one of [LOW, MEDIUM, HIGH, CRITICAL]
            - reasoning: a brief explanation of your decision
            
            CRITICAL risk should be reserved for events like:
            - Fraud / Accounting scandals
            - C-suite arrests
            - Regulatory shutdowns
            - Massive data breaches with confirmed liabilities
            - Bankruptcy filings
            
            HIGH risk for:
            - Missed earnings by wide margin
            - Product recalls
            - Lawsuits
            
            LOW/MEDIUM for routine business news.
            """)
    String evaluateRisk(@UserMessage String newsContent);
}
