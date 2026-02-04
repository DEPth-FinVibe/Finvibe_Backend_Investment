package depth.finvibe.investment.modules.trade.dto;

import depth.finvibe.investment.modules.trade.domain.enums.TradeType;

public enum TradeOrderType {
  NORMAL,
  RESERVED;

  public TradeType toTradeType() {
    return TradeType.valueOf(name());
  }
}
