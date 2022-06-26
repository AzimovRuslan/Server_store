package com.example.springtask.controller;

import com.example.springtask.domain.store.Price;
import com.example.springtask.domain.store.Product;
import com.example.springtask.exceptions.NotFoundException;
import com.example.springtask.repos.PriceRepository;
import com.example.springtask.repos.ProductRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/price")
public class PriceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PriceController.class);
    private final PriceRepository priceRepository;
    private final ProductRepository productRepository;

    public PriceController(PriceRepository priceRepository, ProductRepository productRepository) {
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<Page<Price>> getPrices(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<String> sortBy) {
        Page<Price> prices = priceRepository.findAll(
                PageRequest.of(page.orElse(0),
                        10,
                        Sort.Direction.ASC, sortBy.orElse("id")));

        LOGGER.info("RECEIVED ALL PRICES");
        return ResponseEntity.ok().body(prices);
    }

    @GetMapping("/{value}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<Price>> getPricesByValue(@PathVariable("value") String value) {
        Pattern priceRangePattern = Pattern.compile("price_range-");
        Matcher priceRangeMatcher = priceRangePattern.matcher(value);

        Pattern priceCurrencyPattern = Pattern.compile("currency-");
        Matcher priceCurrencyMatcher = priceCurrencyPattern.matcher(value);
        List<Price> prices = new ArrayList<>();
        if (priceRangeMatcher.find()) {
            int minPrice = Integer.parseInt(value.split("-")[1]);
            int maxPrice = Integer.parseInt(value.split("-")[2]);

            List<Price> pricesFromDb = priceRepository.findAll();
            for (Price price : pricesFromDb) {
                if (price.getConventionalUnit() > minPrice && price.getConventionalUnit() < maxPrice) {
                    prices.add(price);
                }
            }

            LOGGER.info(String.format("RECEIVED ALL PRICES WITH PRICE %d - %d", minPrice, maxPrice));
        } else if (priceCurrencyMatcher.find()) {
            String currency = value.split("-")[1];

            prices = priceRepository.findAll()
                    .stream()
                    .filter(price -> price.getCurrency().equals(currency))
                    .collect(Collectors.toList());

            LOGGER.info(String.format("RECEIVED ALL PRICES WITH CURRENCY  = %s", currency));
        } else if (value.matches("[+]?\\d+")) {
            Price price = priceRepository.getById(Long.parseLong(value));

            Price priceForReturn = new Price();
            priceForReturn.setId(price.getId());
            priceForReturn.setProduct(price.getProduct());
            priceForReturn.setConventionalUnit(price.getConventionalUnit());
            priceForReturn.setCurrency(price.getCurrency());

            prices.add(priceForReturn);

            LOGGER.info(String.format("RECEIVED CATEGORY WITH ID  = %s", value));
        } else {
            prices = priceRepository.findAll()
                    .stream()
                    .filter(price -> price.getProduct().getName().equals(value))
                    .collect(Collectors.toList());

            LOGGER.info(String.format("RECEIVED PRICES WITH PRODUCT NAME = %s", value));
        }

        return ResponseEntity.ok().body(prices);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Price> createPrice(@RequestBody Price price) {
        Price priceForCreate = null;

        Product productFromDb = productRepository.findAll()
                .stream()
                .filter(product -> product.getName().equals(price.getProduct().getName()))
                .findFirst()
                .orElse(null);

        if (productFromDb == null) {
            throw new NotFoundException();
        } else {
            List<Price> pricesFromDb = priceRepository.findAll();
            price.setProduct(productFromDb);

            for (Price p : pricesFromDb) {
                if (p.getProduct().getName().equals(price.getProduct().getName())) {
                    if (p.getCurrency().equals(price.getCurrency())) {
                        priceForCreate = p;
                    }
                }
            }

            if (priceForCreate != null) {
                updatePrice(priceForCreate, price);
            } else {
                priceForCreate = priceRepository.save(price);
            }
        }

        LOGGER.info("ADDED NEW PRICE");
        return ResponseEntity.status(201).body(priceForCreate);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Price> updatePrice(
            @PathVariable("id") Price priceFromDb,
            @RequestBody Price price) {
        price.setProduct(priceFromDb.getProduct());
        BeanUtils.copyProperties(price, priceFromDb, "id");

        LOGGER.info(String.format("UPDATED PRICE WITH ID  = %d", priceFromDb.getId()));
        return ResponseEntity.ok().body(priceRepository.save(priceFromDb));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Price> deletePrice(@PathVariable("id") Long id) {
        Price price = priceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("id-" + id));
        priceRepository.deleteById(id);

        LOGGER.info(String.format("DELETED PRICE WITH ID = %d", id));
        return ResponseEntity.ok().body(price);
    }
}
