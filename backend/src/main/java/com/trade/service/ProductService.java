package com.trade.service;

import com.trade.dto.ProductDTO;
import com.trade.dto.ProductImportResultDTO;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    private static final DataFormatter CELL_FORMAT = new DataFormatter();
    private static final int MAX_IMPORT_ERRORS = 40;

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
            Predicate bar = cb.and(cb.isNotNull(root.get("barcode")), cb.like(root.get("barcode"), kw));
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
        String c = code.trim();
        return productRepository.findByProductCodeOrBarcode(c)
                .orElseThrow(() -> new BusinessException("商品不存在"));
    }

    /**
     * 生成 13 位 EAN-13 形制的内部条码（含校验位），并保证库内唯一。
     */
    public String suggestNewBarcode() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 120; attempt++) {
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 12; i++) {
                sb.append(r.nextInt(10));
            }
            String twelve = sb.toString();
            int check = ean13CheckDigit(twelve);
            String full = twelve + check;
            if (!productRepository.existsByBarcode(full)) {
                return full;
            }
        }
        throw new BusinessException("暂时无法生成唯一条码，请稍后重试");
    }

    private static int ean13CheckDigit(String first12) {
        if (first12 == null || first12.length() != 12) {
            throw new IllegalArgumentException("need 12 digits");
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = first12.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        return (10 - (sum % 10)) % 10;
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
            recordPriceHistory(product, oldPurchase, oldSale, product.getPurchasePrice(), product.getSalePrice(),
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

    public byte[] buildImportTemplateXlsx() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("商品");
            Row h = sh.createRow(0);
            String[] cols = {"商品编码", "商品名称", "分类ID", "单位", "规格", "采购价", "销售价", "保质期(天)", "状态"};
            for (int i = 0; i < cols.length; i++) {
                h.createCell(i).setCellValue(cols[i]);
            }
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("生成模板失败");
        }
    }

    @Transactional
    public ProductImportResultDTO importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Excel 文件");
        }
        ProductImportResultDTO result = new ProductImportResultDTO();
        List<String> errors = result.getErrors();
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException("Excel 无工作表");
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String productCode = cellStr(row, 0);
                if (productCode.isEmpty()) {
                    continue;
                }
                int rowNum = r + 1;
                try {
                    String name = cellStr(row, 1);
                    if (name.isEmpty()) {
                        addErr(errors, "第" + rowNum + "行：商品名称不能为空");
                        result.setFailCount(result.getFailCount() + 1);
                        continue;
                    }
                    Long categoryId = parseLong(row, 2);
                    if (categoryId == null) {
                        addErr(errors, "第" + rowNum + "行：分类ID无效");
                        result.setFailCount(result.getFailCount() + 1);
                        continue;
                    }
                    Optional<Category> catOpt = categoryRepository.findById(categoryId);
                    if (catOpt.isEmpty()) {
                        addErr(errors, "第" + rowNum + "行：分类不存在");
                        result.setFailCount(result.getFailCount() + 1);
                        continue;
                    }
                    String unit = cellStr(row, 3);
                    String spec = cellStr(row, 4);
                    BigDecimal purchase = parseDecimal(row, 5);
                    BigDecimal sale = parseDecimal(row, 6);
                    Integer shelf = parseInt(row, 7);
                    String statusStr = cellStr(row, 8);
                    Product.ProductStatus status = parseStatus(statusStr);
                    /* 模板不含条码列：新建时由系统生成；更新时保留原条码 */
                    String barcode = "";

                    Optional<Product> existing = productRepository.findByProductCode(productCode);
                    if (existing.isPresent()) {
                        Product p = existing.get();
                        BigDecimal op = p.getPurchasePrice();
                        BigDecimal os = p.getSalePrice();
                        p.setName(name);
                        p.setCategory(catOpt.get());
                        p.setUnit(unit.isEmpty() ? null : unit);
                        p.setSpecification(spec.isEmpty() ? null : spec);
                        p.setPurchasePrice(purchase);
                        p.setSalePrice(sale);
                        p.setShelfLife(shelf);
                        p.setStatus(status);
                        applyBarcodeImport(p, barcode, p.getId());
                        boolean pc = !Objects.equals(op, p.getPurchasePrice()) || !Objects.equals(os, p.getSalePrice());
                        if (pc) {
                            recordPriceHistory(p, op, os, p.getPurchasePrice(), p.getSalePrice(),
                                    ProductPriceHistory.ChangeSource.IMPORT);
                        }
                        productRepository.save(p);
                    } else {
                        Product p = new Product();
                        p.setProductCode(productCode);
                        p.setName(name);
                        p.setCategory(catOpt.get());
                        p.setUnit(unit.isEmpty() ? null : unit);
                        p.setSpecification(spec.isEmpty() ? null : spec);
                        p.setPurchasePrice(purchase);
                        p.setSalePrice(sale);
                        p.setShelfLife(shelf);
                        p.setStatus(status);
                        applyBarcodeImport(p, barcode, null);
                        if (p.getBarcode() == null) {
                            p.setBarcode(suggestNewBarcode());
                        }
                        productRepository.save(p);
                    }
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception ex) {
                    addErr(errors, "第" + rowNum + "行：" + ex.getMessage());
                    result.setFailCount(result.getFailCount() + 1);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析 Excel 失败：" + e.getMessage());
        }
        return result;
    }

    private void addErr(List<String> errors, String msg) {
        if (errors.size() < MAX_IMPORT_ERRORS) {
            errors.add(msg);
        }
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
        if (dto.getStatus() != null) {
            product.setStatus(Product.ProductStatus.valueOf(dto.getStatus().trim()));
        } else {
            product.setStatus(Product.ProductStatus.ENABLED);
        }
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
        Category c = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("分类不存在"));
        product.setCategory(c);
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

    private void applyBarcodeImport(Product product, String barcode, Long productIdOrNull) {
        if (barcode == null || barcode.isBlank()) {
            if (productIdOrNull == null) {
                product.setBarcode(null);
            }
            return;
        }
        String b = barcode.trim();
        if (productIdOrNull == null) {
            if (productRepository.existsByBarcode(b)) {
                throw new BusinessException("条码已存在");
            }
        } else {
            if (productRepository.existsByBarcodeAndIdNot(b, productIdOrNull)) {
                throw new BusinessException("条码已存在");
            }
        }
        product.setBarcode(b);
    }

    private void recordPriceHistory(Product product, BigDecimal prevP, BigDecimal prevS,
                                  BigDecimal newP, BigDecimal newS, ProductPriceHistory.ChangeSource source) {
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
            h.setOperatorUsername(null);
        }
        productPriceHistoryRepository.save(h);
    }

    private static String cellStr(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (c == null) {
            return "";
        }
        return CELL_FORMAT.formatCellValue(c).trim();
    }

    private static Long parseLong(Row row, int idx) {
        String s = cellStr(row, idx);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s.replaceAll(",", "").split("\\.")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(Row row, int idx) {
        String s = cellStr(row, idx);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s.replaceAll(",", "").split("\\.")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(Row row, int idx) {
        String s = cellStr(row, idx);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Product.ProductStatus parseStatus(String s) {
        if (s == null || s.isEmpty()) {
            return Product.ProductStatus.ENABLED;
        }
        try {
            return Product.ProductStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Product.ProductStatus.ENABLED;
        }
    }
}
