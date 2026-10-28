package cc.iecuhk.iems5718.s57.service;

import cc.iecuhk.iems5718.s57.entity.Category;
import cc.iecuhk.iems5718.s57.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service // 告诉 Spring Boot 这是一个业务逻辑类，把它放进 IoC 容器的大 Map 里
public class CategoryService {

    @Autowired // 从大 Map 里把 Repository (Dao) 拿过来用
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    public void deleteCategory(Integer id) {
        categoryRepository.deleteById(id);
    }
}