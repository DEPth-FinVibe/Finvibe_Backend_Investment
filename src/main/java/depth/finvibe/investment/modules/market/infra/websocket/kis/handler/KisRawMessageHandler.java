package depth.finvibe.investment.modules.market.infra.websocket.kis.handler;

import depth.finvibe.investment.modules.market.infra.websocket.kis.model.KisMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@RequiredArgsConstructor
public class KisRawMessageHandler extends TextWebSocketHandler {
    private final KisMessageHandler kisMessageHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode rootNode = objectMapper.readTree(payload);
        String pingpongCheck = rootNode.path("header").get("tr_id").toString();

        if(Objects.nonNull(pingpongCheck) && pingpongCheck.equals("PINGPONG")) {
            session.sendMessage(message); // Pong은 받은 메시지를 그대로 다시 보내면 됨.
        }else{
            KisMessage.TransactionResponse response = objectMapper.readValue(payload, KisMessage.TransactionResponse.class);
            kisMessageHandler.handleResponse(response);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        kisMessageHandler.handleError(exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        kisMessageHandler.handleDisconnect();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
    }
}
