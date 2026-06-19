package com.trade.service;

import com.trade.dto.ProductDTO;
import com.trade.entity.Category;
import com.trade.entity.Product;
import com.trade.entity.ProductPriceHistory;
import com.trade.exception.BusinessException;
import com.trade.repository.CategoryRepository;
import com.trade.repository.InventoryRepository;
import com.trade.repository.InventoryTransferLogRepository;
import com.trade.repository.ProductPriceHistoryRepository;
import com.trade.repository.ProductRepository;
import com.trade.repository.PurchaseOrderItemRepository;
import com.trade.repository.SalesOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final InventoryTransferLogRepository inventoryTransferLogRepository;

    @Transactional
    public Product createProduct(ProductDTO productDTO) {
        if (productRepository.existsByProductCode(productDTO.getProductCode().trim())) {
            throw new BusinessException("商品编码已存在");
        }
        Product product = new Product();
        fillProductFromDto(product, productDTO, true);
        return productRepository.save(product);
    }

    public Page<Product> getProducts(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        }
        String kw = "%" + keyword.trim() + "%";
        return productRepository.findAll((root, query, cb) -> {
            Predicate name = cb.like(root.get("name"), kw);
            Predicate code = cb.like(root.get("productCode"), kw);
            Predicate bar  = cb.and(cb.isNotNull(root.get("barcode")), cb.like(root.get("barcode"), kw));
            return cb.or(name, code, bar);
        }, pageable);
    }

    public List<Product> getAllEnabledProducts() {
        return productRepository.findAllEnabled();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("商品不存在"));
    }

    public Product lookupByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("编码不能为空");
        }
        return productRepository.findByProductCodeOrBarcode(code.trim())
                .orElseThrow(() -> new BusinessException("商品不存在"));
    }

    public List<ProductPriceHistory> getPriceHistory(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException("商品不存在");
        }
        return productPriceHistoryRepository.findByProduct_IdOrderByCreatedAtDesc(productId);
    }

    @Transactional
    public Product updateProduct(Long id, ProductDTO productDTO) {
        Product product = getProductById(id);
        BigDecimal oldPurchase = product.getPurchasePrice();
        BigDecimal oldSale = product.getSalePrice();

        String newCode = productDTO.getProductCode().trim();
        if (!product.getProductCode().equals(newCode) && productRepository.existsByProductCode(newCode)) {
            throw new BusinessException("商品编码已存在");
        }
        product.setProductCode(newCode);
        product.setName(productDTO.getName().trim());
        if (productDTO.getUnit() != null) {
            product.setUnit(productDTO.getUnit().trim().isEmpty() ? null : productDTO.getUnit().trim());
        }
        if (productDTO.getSpecification() != null) {
            product.setSpecification(productDTO.getSpecification().trim().isEmpty() ? null : productDTO.getSpecification().trim());
        }
        product.setPurchasePrice(productDTO.getPurchasePrice());
        product.setSalePrice(productDTO.getSalePrice());
        product.setShelfLife(productDTO.getShelfLife());
        if (productDTO.getImageUrl() != null) {
            product.setImageUrl(productDTO.getImageUrl().trim().isEmpty() ? null : productDTO.getImageUrl().trim());
        }
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription().trim().isEmpty() ? null : productDTO.getDescription().trim());
        }
        if (productDTO.getStatus() != null) {
            product.setStatus(Product.ProductStatus.valueOf(productDTO.getStatus().trim()));
        }
        applyCategory(product, productDTO.getCategoryId());
        applyBarcodeUpdate(product, productDTO.getBarcode(), id);

        boolean priceChanged = !Objects.equals(oldPurchase, product.getPurchasePrice())
                || !Objects.equals(oldSale, product.getSalePrice());
        if (priceChanged) {
            recordPriceHistory(product, oldPurchase, oldSale,
                    product.getPurchasePrice(), product.getSalePrice(),
                    ProductPriceHistory.ChangeSource.MANUAL);
        }
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException("商品不存在");
        }
        if (inventoryRepository.existsByProduct_Id(id)) {
            throw new BusinessException("该商品仍存在库存记录，无法删除。请先处理库存或改用「停用」。");
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
        productPriceHistoryRepository.deleteByProduct_Id(id);
        productRepository.deleteById(id);
    }

    /** 生成 EAN-13 内部条码并保证唯一 */
    public String suggestNewBarcode() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 120; attempt++) {
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 12; i++) {
                sb.append(r.nextInt(10));
            }
            String twelve = sb.toString();
            String full = twelve + ean13CheckDigit(twelve);
            if (!productRepository.existsByBarcode(full)) {
                return full;
            }
        }
        throw new BusinessException("暂时无法生成唯一条码，请稍后重试");
    }

    private static int ean13CheckDigit(String first12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = first12.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        return (10 - (sum % 10)) % 10;
    }

    private void fillProductFromDto(Product product, ProductDTO dto, boolean creating) {
        product.setProductCode(dto.getProductCode().trim());
        product.setName(dto.getName().trim());
        if (dto.getUnit() != null) {
            product.setUnit(dto.getUnit().trim().isEmpty() ? null : dto.getUnit().trim());
        }
        if (dto.getSpecification() != null) {
            product.setSpecification(dto.getSpecification().trim().isEmpty() ? null : dto.getSpecification().trim());
        }
        product.setPurchasePrice(dto.getPurchasePrice());
        product.setSalePrice(dto.getSalePrice());
        product.setShelfLife(dto.getShelfLife());
        if (dto.getImageUrl() != null) {
            product.setImageUrl(dto.getImageUrl().trim().isEmpty() ? null : dto.getImageUrl().trim());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription().trim().isEmpty() ? null : dto.getDescription().trim());
        }
        product.setStatus(dto.getStatus() != null
                ? Product.ProductStatus.valueOf(dto.getStatus().trim())
                : Product.ProductStatus.ENABLED);
        applyCategory(product, dto.getCategoryId());
        if (creating) {
            applyBarcodeCreate(product, dto.getBarcode());
        }
    }

    private void applyCategory(Product product, Long categoryId) {
        if (categoryId == null) {
            product.setCategory(null);
            return;
        }
        product.setCategory(categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("分类不存在")));
    }

    private void applyBarcodeCreate(Product product, String barcode) {
        if (barcode == null || barcode.isBlank()) {
            product.setBarcode(suggestNewBarcode());
            return;
        }
        String b = barcode.trim();
        if (productRepository.existsByBarcode(b)) {
            throw new BusinessException("条码已被其他商品使用");
        }
        product.setBarcode(b);
    }

    private void applyBarcodeUpdate(Product product, String barcode, Long productId) {
        if (barcode == null || barcode.isBlank()) {
            product.setBarcode(null);
            return;
        }
        String b = barcode.trim();
        if (productRepository.existsByBarcodeAndIdNot(b, productId)) {
            throw new BusinessException("条码已被其他商品使用");
        }
        product.setBarcode(b);
    }

    private void recordPriceHistory(Product product, BigDecimal prevP, BigDecimal prevS,
                                    BigDecimal newP, BigDecimal newS,
                                    ProductPriceHistory.ChangeSource source) {
        ProductPriceHistory h = new ProductPriceHistory();
        h.setProduct(product);
        h.setPrevPurchasePrice(prevP);
        h.setPrevSalePrice(prevS);
        h.setNewPurchasePrice(newP);
        h.setNewSalePrice(newS);
        h.setSource(source);
        try {
            h.setOperatorUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        } catch (Exception ignored) {
        }
        productPriceHistoryRepository.save(h);
    }
}
