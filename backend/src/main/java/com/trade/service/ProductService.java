package com.trade.service;

import com.trade.dto.ProductDTO;
import com.trade.entity.Product;
import com.trade.exception.BusinessException;
import com.trade.repository.InventoryRepository;
import com.trade.repository.InventoryTransferLogRepository;
import com.trade.repository.ProductRepository;
import com.trade.repository.PurchaseOrderItemRepository;
import com.trade.repository.SalesOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final InventoryTransferLogRepository inventoryTransferLogRepository;

    @Transactional
    public Product createProduct(ProductDTO dto) {
        Product product = new Product();
        product.setProductCode("PRD" + System.currentTimeMillis());
        fillFromDto(product, dto);
        return productRepository.save(product);
    }

    public Page<Product> getProducts(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        }
        String kw = "%" + keyword.trim() + "%";
        return productRepository.findAll((root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.like(root.get("name"), kw));
            ps.add(cb.like(root.get("category"), kw));
            return cb.or(ps.toArray(new Predicate[0]));
        }, pageable);
    }

    public List<Product> getAllEnabledProducts() {
        return productRepository.findAllEnabled();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("商品不存在"));
    }

    @Transactional
    public Product updateProduct(Long id, ProductDTO dto) {
        Product product = getProductById(id);
        fillFromDto(product, dto);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException("商品不存在");
        }
        if (inventoryRepository.existsByProduct_Id(id)) {
            throw new BusinessException("该商品仍存在库存记录，无法删除。可改用「停用」。");
        }
        if (purchaseOrderItemRepository.existsByProduct_Id(id)) {
            throw new BusinessException("该商品已被采购订单引用，无法删除。可改用「停用」。");
        }
        if (salesOrderItemRepository.existsByProduct_Id(id)) {
            throw new BusinessException("该商品已被销售订单引用，无法删除。可改用「停用」。");
        }
        if (inventoryTransferLogRepository.existsByProduct_Id(id)) {
            throw new BusinessException("该商品存在调拨历史记录，无法删除。可改用「停用」。");
        }
        productRepository.deleteById(id);
    }

    private void fillFromDto(Product product, ProductDTO dto) {
        product.setName(dto.getName().trim());
        product.setCategory(emptyToNull(dto.getCategory()));
        product.setUnit(emptyToNull(dto.getUnit()));
        product.setSpecification(emptyToNull(dto.getSpecification()));
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            product.setStatus(Product.ProductStatus.valueOf(dto.getStatus().trim()));
        } else if (product.getId() == null) {
            product.setStatus(Product.ProductStatus.ENABLED);
        }
    }

    private static String emptyToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
