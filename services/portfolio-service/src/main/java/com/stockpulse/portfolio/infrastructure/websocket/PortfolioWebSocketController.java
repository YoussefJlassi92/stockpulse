package com.stockpulse.portfolio.infrastructure.websocket;

import com.stockpulse.portfolio.domain.PortfolioSummaryDto;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class PortfolioWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public PortfolioWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcasts a portfolio summary to all subscribers of {@code /topic/portfolio/{portfolioId}}.
     *
     * @param summary the updated portfolio summary received from the client or an internal source
     */
    @MessageMapping("/portfolio.update")
    public void broadcastPortfolioUpdate(PortfolioSummaryDto summary) {
        messagingTemplate.convertAndSend(
                "/topic/portfolio/" + summary.portfolioId(),
                summary
        );
    }
}
