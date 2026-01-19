package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<Category, Long> {
}
