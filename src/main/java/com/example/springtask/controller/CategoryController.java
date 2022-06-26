package com.example.springtask.controller;

import com.example.springtask.domain.store.Category;
import com.example.springtask.repos.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/category")
public class CategoryController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<Page<Category>> categories(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<String> sortBy
    ) {
        Page<Category> categories = categoryRepository.findAll(
                PageRequest.of(page.orElse(0),
                        10,
                        Sort.Direction.ASC, sortBy.orElse("id")));

        LOGGER.info("RECEIVED ALL CATEGORIES");
        return ResponseEntity.ok().body(categories);
    }

    @GetMapping("/{value}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<Category>> getCategoriesByValue(@PathVariable("value") String value) {
        List<Category> categories = new ArrayList<>();
        if (value != null) {
            if (value.matches("[+]?\\d+")) {
                Category category = categoryRepository.getById(Long.parseLong(value));
                Category categoryForReturn = new Category();
                categoryForReturn.setId(category.getId());
                categoryForReturn.setName(category.getName());
                categories.add(categoryForReturn);

                LOGGER.info(String.format("RECEIVED CATEGORY WITH ID = %s", value));
            } else {
                categories = categoryRepository.findAll()
                        .stream()
                        .filter(category -> category.getName().equals(value))
                        .collect(Collectors.toList());

                LOGGER.info(String.format("RECEIVED CATEGORY WITH NAME = %s", value));
            }
        }
        return ResponseEntity.ok().body(categories);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        if (!category.getSuperCategories().isEmpty()) {
            Category superCategory = getSuperCategoryOfCategory(category);
            List<Category> categoriesFromDb = categoryRepository.findAll();

            for (Category c : categoriesFromDb) {
                if (c.equals(superCategory)){
                    category.getSuperCategories().remove(getSuperCategoryOfCategory(category));
                    category.getSuperCategories().add(c);
                }
            }

            categoryRepository.save(getSuperCategoryOfCategory(category));
        }

        Category categoryForCreate = categoryRepository.findAll()
                .stream()
                .filter(c -> c.equals(category))
                .findFirst()
                .orElse(null);

        if (categoryForCreate != null) {
            category.setName(categoryForCreate.getName());

            if (!categoryForCreate.getSuperCategories().isEmpty()) {
                if (getSuperCategoryOfCategory(categoryForCreate).equals(getSuperCategoryOfCategory(category))) {
                    category.setSuperCategories(categoryForCreate.getSuperCategories());
                } else {
                    category.setSuperCategories(joinLists(category.getSuperCategories(), categoryForCreate.getSuperCategories()));
                }
            }
            updateCategory(categoryForCreate, category);
        } else {
            categoryForCreate = categoryRepository.save(category);
        }

        LOGGER.info(String.format("ADDED NEW CATEGORY"));
        return ResponseEntity.status(201).body(categoryForCreate);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Category> updateCategory(
            @PathVariable("id") Category categoryFromDb,
            @RequestBody Category category) {
        List<Category> categoriesFromDb = categoryRepository.findAll();

        for (Category c : categoriesFromDb) {
            if (c.equals(category)) {
                category.setName(categoryFromDb.getName());
            }
        }

        BeanUtils.copyProperties(category, categoryFromDb, "id");

        LOGGER.info(String.format("UPDATED CATEGORY WITH ID  = %d", categoryFromDb.getId()));
        return ResponseEntity.ok().body(categoryRepository.save(categoryFromDb));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Category> deleteCategory(@PathVariable("id") Long id) {
        Optional<Category> categoryFromDb = categoryRepository.findById(id);
        List<Category> categoriesFromDb = categoryRepository.findAll();
        Category category = categoryFromDb.orElseThrow(() -> new EntityNotFoundException("id-" + id));

        for (Category c : categoriesFromDb) {
            if (!c.getSuperCategories().isEmpty()) {
                if (getSuperCategoryOfCategory(c).equals(category)) {
                    c.getSuperCategories().remove(getSuperCategoryOfCategory(c));
                }
            }
        }

        categoryRepository.deleteById(id);

        LOGGER.info(String.format("DELETED CATEGORY WITH ID  = %d", id));
        return ResponseEntity.ok().body(category);
    }

    private static <T> HashSet<T> joinLists(final Set<T> listA, final Set<T> listB) {
        boolean aEmpty = (listA == null) || listA.isEmpty();
        boolean bEmpty = (listB == null) || listB.isEmpty();

        if (aEmpty && bEmpty) {
            return new HashSet<>();
        } else if (aEmpty) {
            return new HashSet<>(listB);
        } else if (bEmpty) {
            return new HashSet<>(listA);
        } else {
            HashSet<T> result = new HashSet<>(listA.size() + listB.size());
            result.addAll(listA);
            result.addAll(listB);
            return result;
        }
    }

    private static Category getSuperCategoryOfCategory(Category category) {
        return category.getSuperCategories().iterator().next();
    }
}
