package cc.iecuhk.iems5718.s57.repository;

import cc.iecuhk.iems5718.s57.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
}