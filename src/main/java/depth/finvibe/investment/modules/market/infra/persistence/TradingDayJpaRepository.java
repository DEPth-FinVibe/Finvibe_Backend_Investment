package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.domain.TradingDay;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingDayJpaRepository extends JpaRepository<TradingDay, LocalDate> {
}
