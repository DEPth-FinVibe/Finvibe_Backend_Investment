package depth.finvibe.investment.modules.asset.application.port.out;

import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;

public interface PortfolioGroupRepository {
    PortfolioGroup save(PortfolioGroup portfolioGroup);
}
