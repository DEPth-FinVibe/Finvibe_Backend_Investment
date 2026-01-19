package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {
    private final CategoryJpaRepository jpaRepository;

    @Override
    public List<Category> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public boolean existsAny() {
        return jpaRepository.count() > 0;
    }

    @Override
    public List<Category> saveAll(List<Category> categories) {
        return jpaRepository.saveAll(categories);
    }
}
