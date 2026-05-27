package com.tuna.ecommerce.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.Brand;
import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.request.product.ReqCreateProductDTO;
import com.tuna.ecommerce.domain.response.product.ResProductImportDTO;
import com.tuna.ecommerce.repository.AttributeValueRepository;
import com.tuna.ecommerce.repository.BrandRepository;
import com.tuna.ecommerce.repository.CategoryRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductImportService {
    private static final String PRODUCTS_SHEET = "Products";
    private static final String VARIANTS_SHEET = "Variants";
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final AttributeValueRepository attributeValueRepository;

    public byte[] createTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Sheet products = workbook.createSheet(PRODUCTS_SHEET);
            writeHeader(products, headerStyle, "productCode", "name", "categoryName", "brandName",
                    "originalPrice", "costPrice", "stock", "skinType", "attributeValues");
            writeRow(products, 1, "SP001", "Son kem demo", "Son kem", "Tiki", "150000", "90000", "20",
                    "Mọi loại da", "Dung tích=50g; Màu=Đỏ");

            Sheet variants = workbook.createSheet(VARIANTS_SHEET);
            writeHeader(variants, headerStyle, "productCode", "sku", "attributeValues", "priceOverride",
                    "costPrice", "stock", "weight");
            writeRow(variants, 1, "SP001", "SP001-50G-DO", "Dung tích=50g; Màu=Đỏ", "150000", "90000", "10",
                    "200");
            writeRow(variants, 2, "SP001", "SP001-50G-HONG", "Dung tích=50g; Màu=Hồng", "155000", "92000", "10",
                    "200");

            for (int i = 0; i < 9; i++) {
                products.autoSizeColumn(i);
            }
            for (int i = 0; i < 7; i++) {
                variants.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public ResProductImportDTO preview(MultipartFile file) throws IOException {
        ParsedImport parsed = parse(file);
        return parsed.result;
    }

    @Transactional
    public ResProductImportDTO importProducts(MultipartFile file) throws IOException, IdInvalidException {
        ParsedImport parsed = parse(file);
        if (!parsed.result.isValid()) {
            return parsed.result;
        }

        int imported = 0;
        for (ProductRow productRow : parsed.products.values()) {
            ReqCreateProductDTO req = new ReqCreateProductDTO();
            req.setName(productRow.name);
            req.setOriginalPrice(productRow.originalPrice);
            req.setCostPrice(productRow.costPrice);
            req.setStock(productRow.stock);
            req.setCategoryId(productRow.category.getId());
            req.setBrandId(productRow.brand != null ? productRow.brand.getId() : null);
            req.setSkinType(productRow.skinType);
            req.setAttributeValue(productRow.attributeValueIds);
            req.setVariants(productRow.variants);

            productService.handleCreate(req, null);
            imported++;
        }

        parsed.result.setImportedProducts(imported);
        return parsed.result;
    }

    private ParsedImport parse(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            ResProductImportDTO result = new ResProductImportDTO();
            result.getErrors().add(new ResProductImportDTO.RowIssue(PRODUCTS_SHEET, 0, "", "File Excel không hợp lệ"));
            result.setValid(false);
            return new ParsedImport(result, new LinkedHashMap<>());
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            ResProductImportDTO result = new ResProductImportDTO();
            Map<String, ProductRow> products = readProducts(workbook, result);
            readVariants(workbook, products, result);

            result.setValid(result.getErrors().isEmpty());
            result.setValidProducts(products.size());
            result.setValidVariants(products.values().stream().mapToInt(p -> p.variants.size()).sum());
            return new ParsedImport(result, products);
        }
    }

    private Map<String, ProductRow> readProducts(Workbook workbook, ResProductImportDTO result) {
        Sheet sheet = workbook.getSheet(PRODUCTS_SHEET);
        Map<String, ProductRow> products = new LinkedHashMap<>();
        Set<String> productCodes = new HashSet<>();

        if (sheet == null) {
            result.getErrors().add(new ResProductImportDTO.RowIssue(PRODUCTS_SHEET, 0, "", "Thiếu sheet Products"));
            return products;
        }

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }

            result.setProductRows(result.getProductRows() + 1);
            String productCode = text(row, 0);
            String name = text(row, 1);
            String categoryName = text(row, 2);
            String brandName = text(row, 3);

            List<String> rowErrors = new ArrayList<>();
            if (productCode.isBlank()) rowErrors.add("productCode bắt buộc");
            if (name.isBlank()) rowErrors.add("name bắt buộc");
            if (categoryName.isBlank()) rowErrors.add("categoryName bắt buộc");
            if (!productCode.isBlank() && !productCodes.add(productCode)) rowErrors.add("productCode bị trùng trong file");
            if (!name.isBlank() && productRepository.existsByName(name)) rowErrors.add("Tên sản phẩm đã tồn tại");

            Category category = categoryName.isBlank() ? null : categoryRepository.findByName(categoryName);
            if (!categoryName.isBlank() && category == null) rowErrors.add("Không tìm thấy danh mục: " + categoryName);

            Brand brand = brandName.isBlank() ? null : brandRepository.findByName(brandName);
            if (!brandName.isBlank() && brand == null) rowErrors.add("Không tìm thấy thương hiệu: " + brandName);

            BigDecimal originalPrice = money(row, 4);
            double costPrice = decimal(row, 5, 0);
            int stock = integer(row, 6, 0);
            if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                rowErrors.add("originalPrice phải lớn hơn 0");
            }

            List<Long> attributeValueIds = resolveAttributeValues(text(row, 8), rowErrors);

            if (!rowErrors.isEmpty()) {
                addErrors(result, PRODUCTS_SHEET, rowIndex + 1, productCode, rowErrors);
                continue;
            }

            ProductRow product = new ProductRow();
            product.productCode = productCode;
            product.name = name;
            product.category = category;
            product.brand = brand;
            product.originalPrice = originalPrice;
            product.costPrice = costPrice;
            product.stock = stock;
            product.skinType = text(row, 7);
            product.attributeValueIds = attributeValueIds;
            products.put(productCode, product);
        }

        return products;
    }

    private void readVariants(Workbook workbook, Map<String, ProductRow> products, ResProductImportDTO result) {
        Sheet sheet = workbook.getSheet(VARIANTS_SHEET);
        if (sheet == null) {
            return;
        }

        Map<String, Set<String>> skusByProduct = new HashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }

            result.setVariantRows(result.getVariantRows() + 1);
            String productCode = text(row, 0);
            String sku = text(row, 1);
            List<String> rowErrors = new ArrayList<>();

            ProductRow product = products.get(productCode);
            if (productCode.isBlank()) rowErrors.add("productCode bắt buộc");
            if (product == null) rowErrors.add("Không tìm thấy productCode hợp lệ trong sheet Products");
            if (sku.isBlank()) rowErrors.add("sku bắt buộc");
            if (!sku.isBlank() && product != null) {
                Set<String> skus = skusByProduct.computeIfAbsent(productCode, key -> new HashSet<>());
                if (!skus.add(sku)) rowErrors.add("SKU bị trùng trong cùng sản phẩm");
            }

            List<Long> attributeValues = resolveAttributeValues(text(row, 2), rowErrors);
            BigDecimal priceOverride = money(row, 3);
            double costPrice = decimal(row, 4, product != null ? product.costPrice : 0);
            int stock = integer(row, 5, 0);
            double weight = decimal(row, 6, 0);

            if (!rowErrors.isEmpty()) {
                addErrors(result, VARIANTS_SHEET, rowIndex + 1, productCode, rowErrors);
                continue;
            }

            ReqCreateProductDTO.VariantDTO variant = new ReqCreateProductDTO.VariantDTO();
            variant.setSku(sku);
            variant.setPrice(priceOverride);
            variant.setCostPrice(costPrice);
            variant.setStock(stock);
            variant.setWeight(weight);
            variant.setAttributeValues(attributeValues);
            product.variants.add(variant);
        }
    }

    private List<Long> resolveAttributeValues(String raw, List<String> errors) {
        List<Long> ids = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return ids;
        }

        String[] pairs = raw.split(";");
        for (String pair : pairs) {
            String item = pair.trim();
            if (item.isBlank()) continue;

            String[] parts = item.split("=", 2);
            if (parts.length != 2 || parts[0].trim().isBlank() || parts[1].trim().isBlank()) {
                errors.add("Sai định dạng thuộc tính: " + item + " (dùng Tên thuộc tính=Giá trị)");
                continue;
            }

            AttributeValue attributeValue = attributeValueRepository.findByAttributeNameAndValue(parts[0].trim(), parts[1].trim());
            if (attributeValue == null) {
                errors.add("Không tìm thấy thuộc tính: " + item);
            } else {
                ids.add(attributeValue.getId());
            }
        }
        return ids;
    }

    private void addErrors(ResProductImportDTO result, String sheet, int row, String productCode, List<String> errors) {
        for (String error : errors) {
            result.getErrors().add(new ResProductImportDTO.RowIssue(sheet, row, productCode, error));
        }
    }

    private void writeHeader(Sheet sheet, CellStyle style, String... headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeRow(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private boolean isBlankRow(Row row) {
        if (row == null) return true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (!text(row, i).isBlank()) return false;
        }
        return true;
    }

    private String text(Row row, int index) {
        if (row == null) return "";
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private BigDecimal money(Row row, int index) {
        String raw = text(row, index);
        if (raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.replace(".", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double decimal(Row row, int index, double fallback) {
        String raw = text(row, index);
        if (raw.isBlank()) return fallback;
        try {
            return Double.parseDouble(raw.replace(".", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int integer(Row row, int index, int fallback) {
        return (int) Math.max(0, Math.round(decimal(row, index, fallback)));
    }

    private static class ParsedImport {
        private final ResProductImportDTO result;
        private final Map<String, ProductRow> products;

        private ParsedImport(ResProductImportDTO result, Map<String, ProductRow> products) {
            this.result = result;
            this.products = products;
        }
    }

    private static class ProductRow {
        private String productCode;
        private String name;
        private Category category;
        private Brand brand;
        private BigDecimal originalPrice;
        private double costPrice;
        private int stock;
        private String skinType;
        private List<Long> attributeValueIds = new ArrayList<>();
        private List<ReqCreateProductDTO.VariantDTO> variants = new ArrayList<>();
    }
}
