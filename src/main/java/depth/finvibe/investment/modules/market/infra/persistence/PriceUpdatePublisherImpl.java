package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.PriceUpdatePublisher;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class PriceUpdatePublisherImpl implements PriceUpdatePublisher {

    @Override
    public void publishToSessions(Set<String> sessionIds, String topic, CurrentPriceDto.Response priceUpdate) {

    }

    @Override
    public void publishBulkPriceUpdate(List<CurrentPriceDto.Response> priceUpdates) {

    }
}
