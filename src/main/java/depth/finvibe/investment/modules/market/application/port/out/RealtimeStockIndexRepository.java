package depth.finvibe.investment.modules.market.application.port.out;

import java.util.UUID;

public interface RealtimeStockIndexRepository {

    // (stockid, watcherId) 쌍으로 캐시에 저장 (ttl을 줘서) ttl -> 일반유저쪽 websocket 세션 유지시간
    //                                                    -> 여기있는 레코드들은 유저가 실시간 watch를 원하는 주식들에대한 티켓 역할도 함

    // 일반 유저가 특정 종목에 대해 watch를 하려면 RealtimeStockIndex에 (stockid, watcherId) 를 등록해야 watch 가능
    // -> 웹소켓 세션과 1차캐시 인덱스 데이터와 데이터 동기화(정합성)을 가져가겠다
    // -> CurrentPrice모델에 이걸 통합하면 일단 이상함.

    //A유저가 보고있는 동시에 B유저도 보고있을수 있음

    // stockId가 존재하냐 존재하지 않냐 여부 -> 이 stockId가 실시간 추적중이냐 아니냐? 로 판단할수 있음

    // 일단 TTL관리를 레디스 인프라쪽으로 떠넘길수 있음, 그리고 카운트를 세는것은 너무 낙관적인 방식. 멱등성이 있는 api를 설계 불가능



    void addRealtimeStockIndex(Long stockId, UUID watcherId); // -> 유저가 이 스톡을 실시간 조회하고있다.
    void renewRealtimeStockIndex(Long stockId, UUID watcherId);
    void removeRealtimeStockIndex(Long stockId, UUID watcherId);

    boolean existsByStockId(Long stockId); // -> 이 스톡이 실시간 조회되고있냐? -> 이 스톡에 대해 한국투자증권과 실시간으로 연결해야하냐?
    boolean allExistsByStockIds(Iterable<Long> stockIds);
}