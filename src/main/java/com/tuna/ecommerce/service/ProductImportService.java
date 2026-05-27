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
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
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

            // --- Styles ---
            CellStyle headerStyle = createHeaderStyle(workbook, IndexedColors.DARK_BLUE, IndexedColors.WHITE);
            CellStyle subHeaderStyle = createHeaderStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE,
                    IndexedColors.BLACK);
            CellStyle sampleStyle = workbook.createCellStyle();
            sampleStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            sampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(sampleStyle);

            // --- Fetch reference data ---
            List<String> categoryNames = categoryRepository.findAll().stream()
                    .map(Category::getName).sorted().collect(Collectors.toList());
            List<String> brandNames = brandRepository.findAll().stream()
                    .map(Brand::getName).sorted().collect(Collectors.toList());
            List<AttributeValue> allAttrValues = attributeValueRepository.findAll();

            // === 1. INSTRUCTIONS SHEET ===
            createInstructionsSheet(workbook, headerStyle, categoryNames, brandNames, allAttrValues);

            // === 2. PRODUCTS SHEET ===
            Sheet products = workbook.createSheet(PRODUCTS_SHEET);
            String[] productHeaders = { "productCode (*)", "name (*)", "categoryName (*)", "brandName",
                    "originalPrice (*)", "costPrice", "stock", "skinType", "attributeValues" };
            writeStyledHeader(products, headerStyle, productHeaders);

            // Description row
            String[] productDesc = { "Mã SP duy nhất", "Tên sản phẩm", "Tên danh mục", "Tên thương hiệu",
                    "Giá bán (VNĐ)", "Giá vốn (VNĐ)", "Số lượng tồn", "Loại da", "Tên=Giá trị; ..." };
            writeStyledRow(products, 1, subHeaderStyle, productDesc);

            // Sample data
            writeStyledRow(products, 2, sampleStyle, "1", "Kem chống nắng Anessa", "Kem chống nắng", "Anessa",
                    "450000", "280000", "50", "Mọi loại da", "Dung tích=60ml");
            writeStyledRow(products, 3, sampleStyle, "2", "Son kem lì 3CE Velvet", "Son kem", "3CE",
                    "350000", "180000", "100", "", "Màu=Đỏ; Dung tích=4g");
            writeStyledRow(products, 4, sampleStyle, "3", "Sữa rửa mặt CeraVe", "Sữa rửa mặt", "CeraVe",
                    "320000", "200000", "80", "Da dầu", "Dung tích=236ml");

            // Data validation dropdowns
            if (!categoryNames.isEmpty()) {
                addDropdownValidation(products, 4, 500, 2, 2, categoryNames);
            }
            if (!brandNames.isEmpty()) {
                addDropdownValidation(products, 4, 500, 3, 3, brandNames);
            }

            autoSizeColumns(products, productHeaders.length);

            // === 3. VARIANTS SHEET ===
            Sheet variants = workbook.createSheet(VARIANTS_SHEET);
            String[] variantHeaders = { "productCode (*)", "sku (*)", "attributeValues (*)", "priceOverride",
                    "costPrice", "stock", "weight (gram)" };
            writeStyledHeader(variants, headerStyle, variantHeaders);

            String[] variantDesc = { "Mã SP từ sheet Products", "Mã SKU duy nhất", "Tên=Giá trị; ...",
                    "Giá riêng (VNĐ)", "Giá vốn (VNĐ)", "Tồn kho riêng", "Trọng lượng (gram)" };
            writeStyledRow(variants, 1, subHeaderStyle, variantDesc);

            // Sample variant data
            writeStyledRow(variants, 2, sampleStyle, "2", "SP002-DO-4G", "Màu=Đỏ; Dung tích=4g",
                    "350000", "180000", "30", "50");
            writeStyledRow(variants, 3, sampleStyle, "2", "SP002-HONG-4G", "Màu=Hồng; Dung tích=4g",
                    "350000", "180000", "35", "50");
            writeStyledRow(variants, 4, sampleStyle, "2", "SP002-CAM-4G", "Màu=Cam; Dung tích=4g",
                    "360000", "185000", "35", "50");
            writeStyledRow(variants, 5, sampleStyle, "3", "SP003-236ML", "Dung tích=236ml",
                    "320000", "200000", "40", "300");
            writeStyledRow(variants, 6, sampleStyle, "3", "SP003-473ML", "Dung tích=473ml",
                    "520000", "350000", "40", "550");

            autoSizeColumns(variants, variantHeaders.length);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createInstructionsSheet(Workbook workbook, CellStyle titleStyle,
            List<String> categoryNames, List<String> brandNames, List<AttributeValue> attrValues) {
        Sheet sheet = workbook.createSheet("Hướng dẫn");

        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 11);
        boldStyle.setFont(boldFont);

        int r = 0;
        setCellValue(sheet, r++, 0, "HƯỚNG DẪN IMPORT SẢN PHẨM BẰNG EXCEL", titleStyle);
        r++;
        setCellValue(sheet, r++, 0, "1. TỔNG QUAN", boldStyle);
        setCellValue(sheet, r++, 0,
                "- File gồm 2 sheet: \"Products\" (thông tin sản phẩm) và \"Variants\" (biến thể sản phẩm).");
        setCellValue(sheet, r++, 0,
                "- Dòng 1: Tiêu đề cột (KHÔNG SỬA). Dòng 2: Mô tả cột. Dòng 3+: Dữ liệu mẫu (XOÁ trước khi nhập).");
        setCellValue(sheet, r++, 0, "- Các cột có dấu (*) là BẮT BUỘC.");
        r++;

        setCellValue(sheet, r++, 0, "2. SHEET \"PRODUCTS\" - THÔNG TIN SẢN PHẨM", boldStyle);
        setCellValue(sheet, r++, 0,
                "productCode (*): Mã sản phẩm do bạn tự đặt, dùng để liên kết với sheet Variants. VD: 1, 2");
        setCellValue(sheet, r++, 0, "name (*): Tên sản phẩm. Không được trùng với sản phẩm đã có trong hệ thống.");
        setCellValue(sheet, r++, 0,
                "categoryName (*): Tên danh mục (chọn từ dropdown). Phải khớp chính xác với danh mục trong hệ thống.");
        setCellValue(sheet, r++, 0, "brandName: Tên thương hiệu (chọn từ dropdown). Có thể để trống.");
        setCellValue(sheet, r++, 0, "originalPrice (*): Giá bán gốc (VNĐ). Chỉ nhập số, không nhập đơn vị. VD: 450000");
        setCellValue(sheet, r++, 0, "costPrice: Giá vốn (VNĐ). Dùng để tính lợi nhuận. Có thể để trống.");
        setCellValue(sheet, r++, 0, "stock: Tổng số lượng tồn kho. Mặc định là 0 nếu để trống.");
        setCellValue(sheet, r++, 0, "skinType: Loại da phù hợp. VD: Da dầu, Da khô, Mọi loại da. Có thể để trống.");
        setCellValue(sheet, r++, 0,
                "attributeValues: Thuộc tính sản phẩm, theo định dạng: Tên thuộc tính=Giá trị; ...");
        setCellValue(sheet, r++, 0, "   VD: Dung tích=50ml; Màu=Đỏ   (phân tách bằng dấu chấm phẩy)");
        r++;

        setCellValue(sheet, r++, 0, "3. SHEET \"VARIANTS\" - BIẾN THỂ SẢN PHẨM", boldStyle);
        setCellValue(sheet, r++, 0, "- Mỗi sản phẩm có thể có NHIỀU biến thể (VD: nhiều màu, nhiều dung tích).");
        setCellValue(sheet, r++, 0, "- Nếu sản phẩm KHÔNG có biến thể, KHÔNG CẦN điền sheet này.");
        setCellValue(sheet, r++, 0, "productCode (*): Mã SP từ sheet Products. Phải khớp chính xác.");
        setCellValue(sheet, r++, 0, "sku (*): Mã SKU duy nhất cho biến thể. VD: 1-Đỏ-4g");
        setCellValue(sheet, r++, 0, "attributeValues (*): Thuộc tính của biến thể. VD: Màu=Đỏ; Dung tích=4g");
        setCellValue(sheet, r++, 0,
                "priceOverride: Giá bán riêng cho biến thể. Nếu để trống, sẽ dùng giá của sản phẩm gốc.");
        setCellValue(sheet, r++, 0, "costPrice: Giá vốn riêng cho biến thể.");
        setCellValue(sheet, r++, 0, "stock: Số lượng tồn kho riêng cho biến thể.");
        setCellValue(sheet, r++, 0, "weight (gram): Trọng lượng biến thể tính bằng gram. VD: 200");
        r++;

        setCellValue(sheet, r++, 0, "4. DANH MỤC CÓ SẴN TRONG HỆ THỐNG", boldStyle);
        setCellValue(sheet, r++, 0,
                categoryNames.isEmpty() ? "(Chưa có danh mục nào)" : String.join(", ", categoryNames));
        r++;

        setCellValue(sheet, r++, 0, "5. THƯƠNG HIỆU CÓ SẴN TRONG HỆ THỐNG", boldStyle);
        setCellValue(sheet, r++, 0, brandNames.isEmpty() ? "(Chưa có thương hiệu nào)" : String.join(", ", brandNames));
        r++;

        setCellValue(sheet, r++, 0, "6. THUỘC TÍNH CÓ SẴN TRONG HỆ THỐNG", boldStyle);
        if (attrValues.isEmpty()) {
            setCellValue(sheet, r++, 0, "(Chưa có thuộc tính nào)");
        } else {
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (AttributeValue av : attrValues) {
                String attrName = av.getAttribute() != null ? av.getAttribute().getName() : "Khác";
                grouped.computeIfAbsent(attrName, k -> new ArrayList<>()).add(av.getAttributeValue());
            }
            for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
                setCellValue(sheet, r++, 0, entry.getKey() + ": " + String.join(", ", entry.getValue()));
            }
        }

        sheet.setColumnWidth(0, 100 * 256);
    }

    private CellStyle createHeaderStyle(Workbook workbook, IndexedColors bgColor, IndexedColors fontColor) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(fontColor.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void setCellValue(Sheet sheet, int rowIndex, int colIndex, String value) {
        setCellValue(sheet, rowIndex, colIndex, value, null);
    }

    private void setCellValue(Sheet sheet, int rowIndex, int colIndex, String value, CellStyle style) {
        Row row = sheet.getRow(rowIndex);
        if (row == null)
            row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        if (style != null)
            cell.setCellStyle(style);
    }

    private void writeStyledHeader(Sheet sheet, CellStyle style, String... headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeStyledRow(Sheet sheet, int index, CellStyle style, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            if (style != null)
                cell.setCellStyle(style);
        }
    }

    private void addDropdownValidation(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol,
            List<String> values) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        String[] arr = values.toArray(new String[0]);
        DataValidationConstraint constraint = helper.createExplicitListConstraint(arr);
        CellRangeAddressList range = new CellRangeAddressList(firstRow, lastRow, firstCol, lastCol);
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
        validation.createErrorBox("Giá trị không hợp lệ", "Vui lòng chọn từ danh sách có sẵn.");
        sheet.addValidationData(validation);
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(currentWidth, 4000));
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

        int startRow = 1;
        Row maybeDescRow = sheet.getRow(1);
        if (maybeDescRow != null && "Mã SP duy nhất".equals(text(maybeDescRow, 0))) {
            startRow = 2;
        }

        for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
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
            if (productCode.isBlank())
                rowErrors.add("productCode bắt buộc");
            if (name.isBlank())
                rowErrors.add("name bắt buộc");
            if (categoryName.isBlank())
                rowErrors.add("categoryName bắt buộc");
            if (!productCode.isBlank() && !productCodes.add(productCode))
                rowErrors.add("productCode bị trùng trong file");
            if (!name.isBlank() && productRepository.existsByName(name))
                rowErrors.add("Tên sản phẩm đã tồn tại");

            Category category = categoryName.isBlank() ? null : categoryRepository.findByName(categoryName);
            if (!categoryName.isBlank() && category == null)
                rowErrors.add("Không tìm thấy danh mục: " + categoryName);

            Brand brand = brandName.isBlank() ? null : brandRepository.findByName(brandName);
            if (!brandName.isBlank() && brand == null)
                rowErrors.add("Không tìm thấy thương hiệu: " + brandName);

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
        int startRow = 1;
        Row maybeDescRow = sheet.getRow(1);
        if (maybeDescRow != null && "Mã SP từ sheet Products".equals(text(maybeDescRow, 0))) {
            startRow = 2;
        }

        for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }

            result.setVariantRows(result.getVariantRows() + 1);
            String productCode = text(row, 0);
            String sku = text(row, 1);
            List<String> rowErrors = new ArrayList<>();

            ProductRow product = products.get(productCode);
            if (productCode.isBlank())
                rowErrors.add("productCode bắt buộc");
            if (product == null)
                rowErrors.add("Không tìm thấy productCode hợp lệ trong sheet Products");
            if (sku.isBlank())
                rowErrors.add("sku bắt buộc");
            if (!sku.isBlank() && product != null) {
                Set<String> skus = skusByProduct.computeIfAbsent(productCode, key -> new HashSet<>());
                if (!skus.add(sku))
                    rowErrors.add("SKU bị trùng trong cùng sản phẩm");
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
            if (item.isBlank())
                continue;

            String[] parts = item.split("=", 2);
            if (parts.length != 2 || parts[0].trim().isBlank() || parts[1].trim().isBlank()) {
                errors.add("Sai định dạng thuộc tính: " + item + " (dùng Tên thuộc tính=Giá trị)");
                continue;
            }

            AttributeValue attributeValue = attributeValueRepository.findByAttributeNameAndValue(parts[0].trim(),
                    parts[1].trim());
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
        if (row == null)
            return true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (!text(row, i).isBlank())
                return false;
        }
        return true;
    }

    private String text(Row row, int index) {
        if (row == null)
            return "";
        Cell cell = row.getCell(index);
        if (cell == null)
            return "";
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private BigDecimal money(Row row, int index) {
        String raw = text(row, index);
        if (raw.isBlank())
            return null;
        try {
            return new BigDecimal(raw.replace(".", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double decimal(Row row, int index, double fallback) {
        String raw = text(row, index);
        if (raw.isBlank())
            return fallback;
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
