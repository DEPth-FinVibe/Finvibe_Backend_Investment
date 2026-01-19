package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.domain.Category;
import java.util.List;

public interface CategoryCommandUseCase {
    boolean existsAny();

    void bulkInsert(List<Category> categories);
}
