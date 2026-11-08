package com.boilerplate.domain.products;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Persistence port for the {@link Product} aggregate. The {@link Sku} value object is
 * unwrapped to its {@code String} at this boundary so JPQL string functions work on it.
 */
public interface ProductRepository extends Repository<Product, UUID> {

    Optional<Product> findById(UUID id);

    default Optional<Product> findBySku(Sku sku) {
        return findBySkuValue(sku.value());
    }

    default boolean existsBySku(Sku sku) {
        return existsBySkuValue(sku.value());
    }

    @Query("SELECT p FROM Product p WHERE p.sku = :sku")
    Optional<Product> findBySkuValue(@Param("sku") String sku);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END FROM Product p WHERE p.sku = :sku")
    boolean existsBySkuValue(@Param("sku") String sku);

    List<Product> findByIdIn(Collection<UUID> ids);

    /** {@code search} is never null — an empty string matches everything. */
    @Query("SELECT p FROM Product p WHERE "
            + "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> search(@Param("search") String search, Pageable pageable);

    long count();

    long countByStockQuantityLessThanEqual(int threshold);

    Product save(Product product);
}
