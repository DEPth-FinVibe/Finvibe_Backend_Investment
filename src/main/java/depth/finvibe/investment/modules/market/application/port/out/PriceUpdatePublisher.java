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

// 1. 브라우저측에서 우리한테 요청하는 웹소켓 -> inward port
// 얜 실질적으로 실시간성을 고려할 필요가 없음.
// 인프라 알아서 관리하다가 특정 stockid에 대한 가격이 필요하면 알아서 호출하라 하면 됨
// -> 인터페이스가 subscibe이런거 필요가 없음. 그냥 getData(stockId) 이런식으로 하면 됨


// 2. 우리서버에서 한국투자증권 서버로 요청할때 쓰는 웹소켓 -> outward port
// market에서 옵저버 패턴으로 인프라쪽에 던져서 옵저버에 있는 메서드 호출하라 하고

/**

 interface ManagingStockGroup {
   Set<Long> getManagedStockIds();
 }

 interface StockObserver {

   ManagingStockGroup getManagingStockGroup();

   void onPriceUpdate(CurrentPriceDto.Response priceUpdate);

 }

 interface PriceUpdateSubscriber {

   void subscribe(StockObserver observer);

   void unsubscribe(Long stockId);

 }
 */
