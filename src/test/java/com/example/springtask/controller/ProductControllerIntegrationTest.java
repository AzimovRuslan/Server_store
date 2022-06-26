package com.example.springtask.controller;

import com.example.springtask.domain.store.Category;
import com.example.springtask.domain.store.Product;
import com.example.springtask.repos.CategoryRepository;
import com.example.springtask.repos.PriceRepository;
import com.example.springtask.repos.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    public void resetDb() {
        priceRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void whenCreateProduct_thenStatus201() {
        Category superCategory = new Category("Outwear");
        ResponseEntity<Category> responseSuperCategory = restTemplate
                .withBasicAuth("admin", "admin")
                .postForEntity("/category", superCategory, Category.class);

        Set<Category> categories = new HashSet<>();
        categories.add(superCategory);

        Category category = new Category("Jackets");
        category.setSuperCategories(categories);
        ResponseEntity<Category> responseCategory = restTemplate
                .withBasicAuth("admin", "admin")
                .postForEntity("/category", category, Category.class);

        Product product = new Product("Jacket");
        product.setCategory(category);
        ResponseEntity<Product> responseProduct = restTemplate
                .withBasicAuth("admin", "admin")
                .postForEntity("/product", product, Product.class);

        assertThat(responseProduct.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(Objects.requireNonNull(responseProduct.getBody()).getId(), notNullValue());
        assertThat(responseProduct.getBody().getName(), is("Jacket"));
        assertThat(responseProduct.getBody().getCategory().getName(), is("Jackets"));
    }

    @Test
    void whenGetProductById_thenStatus200() {
        Long id = getTestProduct().getId();
        Product[] products = restTemplate
                .withBasicAuth("user", "user")
                .getForObject("/product/{id}", Product[].class, id);

        assertThat(products[0].getName(), is("Jacket"));
    }

    @Test
    void whenGetProductByName_thenStatus200() {
        String value = getTestProduct().getName();
        Product[] products = restTemplate
                .withBasicAuth("user", "user")
                .getForObject("/product/{value}", Product[].class, value);

        assertThat(products[0].getName(), is("Jacket"));
    }

    @Test
    void whenGetProductByCategoryId_thenStatus200() {
        String value = "category_id-" + getTestProduct().getCategory().getId();
        Product[] products = restTemplate
                .withBasicAuth("user", "user")
                .getForObject("/product/{value}", Product[].class, value);

        assertThat(products[0].getName(), is("Jacket"));
    }

    @Test
    void whenUpdateProduct_thenStatus200() {
        Category category = new Category("Outwear");
        Long id = getTestProduct().getId();

        Product product = new Product("Vest");
        product.setCategory(category);

        HttpEntity<Product> entity = new HttpEntity<>(product);

        ResponseEntity<Product> responseEntity = restTemplate
                .withBasicAuth("admin", "admin")
                .exchange("/product/{id}", HttpMethod.PUT, entity,
                Product.class, id);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(responseEntity.getBody()).getId(), notNullValue());
        assertThat(responseEntity.getBody().getName(), is("Vest"));
    }

    @Test
    void givenProduct_whenDeleteCategory_thenStatus200() {
        Long id = getTestProduct().getId();

        ResponseEntity<Product> responseEntity = restTemplate
                .withBasicAuth("admin", "admin")
                .exchange("/product/{id}", HttpMethod.DELETE, null,
                Product.class, id);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(responseEntity.getBody()).getId(), notNullValue());
        assertThat(responseEntity.getBody().getName(), is("Jacket"));
    }

    private Product createTestProduct(Category category, String name) {
        categoryRepository.save(category);
        Product product = new Product(name);
        product.setCategory(category);
        return productRepository.save(product);
    }

    private Product getTestProduct() {
        Category category = new Category("Jackets");
        return createTestProduct(category, "Jacket");
    }
}
