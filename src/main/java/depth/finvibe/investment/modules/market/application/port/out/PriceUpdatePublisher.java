package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;

import java.util.List;
import java.util.Set;

/**
 * 실시간 가격 업데이트를 클라이언트에게 발행하는 포트
 * WebSocket 등의 실시간 통신을 위한 아웃바운드 포트
 */
public interface PriceUpdatePublisher {

    void publishToSessions(Set<String> sessionIds, String topic, CurrentPriceDto.Response priceUpdate);

    void publishBulkPriceUpdate(List<CurrentPriceDto.Response> priceUpdates);
}