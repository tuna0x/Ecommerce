package com.tuna.ecommerce.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.response.dashboard.ResDashboardDTO;

@Service
public class DashboardExcelService {

    public byte[] exportStatisticsToExcel(ResDashboardDTO data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Báo cáo Thống kê");

            // Define styles
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0\"đ\""));

            // 1. Overview Section
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO TỔNG QUAN KINH DOANH");
            
            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Chỉ số", "Giá trị"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            addRow(sheet, rowIdx++, "Tổng doanh thu", data.getTotalRevenue(), currencyStyle);
            addRow(sheet, rowIdx++, "Tổng đơn hàng", (double) data.getTotalOrders(), null);
            addRow(sheet, rowIdx++, "Tổng khách hàng", (double) data.getTotalUsers(), null);
            addRow(sheet, rowIdx++, "AOV (Trung bình đơn)", data.getAverageOrderValue(), currencyStyle);
            addRow(sheet, rowIdx++, "Tỷ lệ tăng trưởng (%)", data.getRevenueGrowthRate(), null);
            addRow(sheet, rowIdx++, "Khách hàng mới", (double) data.getNewUsersCount(), null);
            addRow(sheet, rowIdx++, "Khách quay lại", (double) data.getReturningUsersCount(), null);

            rowIdx += 2; // Spacing

            // 2. Order Status Distribution
            Row statusTitleRow = sheet.createRow(rowIdx++);
            statusTitleRow.createCell(0).setCellValue("PHÂN BỔ TRẠNG THÁI ĐƠN HÀNG");
            
            Row statusHeaderRow = sheet.createRow(rowIdx++);
            String[] statusHeaders = {"Trạng thái", "Số lượng"};
            for (int i = 0; i < statusHeaders.length; i++) {
                Cell cell = statusHeaderRow.createCell(i);
                cell.setCellValue(statusHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            for (Map.Entry<String, Long> entry : data.getOrderStatusDistribution().entrySet()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }

            rowIdx += 2; // Spacing

            // 3. Top Selling Products
            Row productTitleRow = sheet.createRow(rowIdx++);
            productTitleRow.createCell(0).setCellValue("TOP SẢN PHẨM BÁN CHẠY");
            
            Row productHeaderRow = sheet.createRow(rowIdx++);
            String[] productHeaders = {"Tên sản phẩm", "Số lượng bán"};
            for (int i = 0; i < productHeaders.length; i++) {
                Cell cell = productHeaderRow.createCell(i);
                cell.setCellValue(productHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            for (ResDashboardDTO.ProductStat product : data.getTopSellingProducts()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(product.getName());
                row.createCell(1).setCellValue(product.getQuantity());
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void addRow(Sheet sheet, int rowIdx, String label, Object value, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        if (value instanceof Number) {
            valueCell.setCellValue(((Number) value).doubleValue());
        } else {
            valueCell.setCellValue(value != null ? value.toString() : "");
        }
        if (style != null) {
            valueCell.setCellStyle(style);
        }
    }
}
