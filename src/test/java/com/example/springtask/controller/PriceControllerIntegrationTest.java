package com.example.springtask.controller;

import com.example.springtask.domain.store.Category;
import com.example.springtask.domain.store.Price;
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
class PriceControllerIntegrationTest {
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
    void whenCreatePrice_thenStatus201() {
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

        Price price = new Price(100, "BYN");
        price.setProduct(product);
        ResponseEntity<Price> responsePrice = restTemplate
                .withBasicAuth("admin", "admin")
                .postForEntity("/price", price, Price.class);

        assertThat(responsePrice.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(Objects.requireNonNull(responsePrice.getBody()).getId(), notNullValue());
        assertThat(responsePrice.getBody().getConventionalUnit(), is(100));
        assertThat(responsePrice.getBody().getCurrency(), is("BYN"));
        assertThat(responsePrice.getBody().getProduct().getName(), is("Jacket"));
        assertThat(responsePrice.getBody().getProduct().getCategory().getName(), is("Jackets"));
        assertThat(responsePrice.getBody().getProduct()
                .getCategory()
                .getSuperCategories()
                .iterator()
                .next()
                .getName(), is("Outwear"));
    }

    @Test
    void whenGetPriceByCurrency_thenStatus200() {
        String currency = getTestPrice().getCurrency();
        String value = "currency-" + currency;

        Price[] prices = restTemplate
                .withBasicAuth("user", "user")
                .getForObject("/price/{value}", Price[].class, value);

        assertThat(prices[0].getCurrency(), is("BYN"));
        assertThat(prices[0].getConventionalUnit(), is(100));
    }

    @Test
    void whenGetPriceByPriceRange_thenStatus200() {
        Price price = getTestPrice();
        ResponseEntity<Price> responsePrice = restTemplate.postForEntity("/price", price, Price.class);

        String value = "price_range-99-110";

        Price[] prices = restTemplate
                .withBasicAuth("user", "user")
                .getForObject("/price/{value}", Price[].class, value);

        assertThat(prices[0].getCurrency(), is("BYN"));
        assertThat(prices[0].getConventionalUnit(), is(100));
    }

    @Test
    void whenGetProductById_thenStatus200() {
        Long id = getTestPrice().getId();
        Price[] prices = restTemplate
                .withBasicAuth("user", "user")
                .getForObject("/price/{id}", Price[].class, id);

        assertThat(prices[0].getConventionalUnit(), is(100));
        assertThat(prices[0].getCurrency(), is("BYN"));
    }

    @Test
    void whenUpdateProduct_thenStatus200() {
        Long id = getTestPrice().getId();

        Category category = new Category("Jackets");
        Product product = new Product("Jacket");
        product.setCategory(category);
        Price price = new Price(200, "EUR");
        price.setProduct(product);

        HttpEntity<Price> entity = new HttpEntity<>(price);

        ResponseEntity<Price> responseEntity = restTemplate
                .withBasicAuth("admin", "admin")
                .exchange("/price/{id}", HttpMethod.PUT, entity, Price.class, id);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(responseEntity.getBody()).getId(), notNullValue());
        assertThat(responseEntity.getBody().getConventionalUnit(), is(200));
        assertThat(responseEntity.getBody().getCurrency(), is("EUR"));
    }

    @Test
    void givenProduct_whenDeleteCategory_thenStatus200() {
        Long id = getTestPrice().getId();

        ResponseEntity<Price> responseEntity = restTemplate
                .withBasicAuth("admin", "admin")
                .exchange("/price/{id}", HttpMethod.DELETE, null,
                Price.class, id);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(responseEntity.getBody()).getId(), notNullValue());
        assertThat(responseEntity.getBody().getConventionalUnit(), is(100));
        assertThat(responseEntity.getBody().getCurrency(), is("BYN"));
        assertThat(responseEntity.getBody().getProduct().getName(), is("Jacket"));
    }

    private Price createTestPrice(Category category, String productName, int conventionalUnit, String currency) {
        categoryRepository.save(category);

        Product product = new Product(productName);
        product.setCategory(category);
        productRepository.save(product);

        Price price = new Price(conventionalUnit, currency);
        price.setProduct(product);

        return priceRepository.save(price);
    }

    private Price getTestPrice() {
        Category category = new Category("Jackets");
        return createTestPrice(category, "Jacket", 100, "BYN");
    }
}
