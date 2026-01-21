package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.Category;

import java.util.List;

public interface CategoryRepository {
    List<Category> findAll();

    boolean existsAny();

    List<Category> saveAll(List<Category> categories);
}
