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

            // ---------------------------------------------------------
            // SHEET 1: DOANH THU & ĐƠN HÀNG
            // ---------------------------------------------------------
            Sheet salesSheet = workbook.createSheet("Doanh thu & Đơn hàng");
            int s1Row = 0;

            // Title
            Row t1Row = salesSheet.createRow(s1Row++);
            t1Row.createCell(0).setCellValue("BÁO CÁO KINH DOANH CHI TIẾT");

            // 1. Overview Section
            Row h1Row = salesSheet.createRow(s1Row++);
            String[] h1 = { "Chỉ số", "Giá trị" };
            for (int i = 0; i < h1.length; i++) {
                Cell cell = h1Row.createCell(i);
                cell.setCellValue(h1[i]);
                cell.setCellStyle(headerStyle);
            }

            addRow(salesSheet, s1Row++, "Tổng doanh thu", data.getTotalRevenue(), currencyStyle);
            addRow(salesSheet, s1Row++, "Tổng đơn hàng", (double) data.getTotalOrders(), null);
            addRow(salesSheet, s1Row++, "Tổng khách hàng", (double) data.getTotalUsers(), null);
            addRow(salesSheet, s1Row++, "AOV (Trung bình đơn)", data.getAverageOrderValue(), currencyStyle);
            addRow(salesSheet, s1Row++, "Tỷ lệ tăng trưởng (%)", data.getRevenueGrowthRate(), null);
            addRow(salesSheet, s1Row++, "Khách hàng mới", (double) data.getNewUsersCount(), null);
            addRow(salesSheet, s1Row++, "Khách quay lại", (double) data.getReturningUsersCount(), null);

            s1Row += 2; // Spacing

            // 2. Order Status Distribution
            salesSheet.createRow(s1Row++).createCell(0).setCellValue("PHÂN BỔ TRẠNG THÁI ĐƠN HÀNG");
            Row hsRow = salesSheet.createRow(s1Row++);
            String[] hs = { "Trạng thái", "Số lượng" };
            for (int i = 0; i < hs.length; i++) {
                Cell cell = hsRow.createCell(i);
                cell.setCellValue(hs[i]);
                cell.setCellStyle(headerStyle);
            }
            if (data.getOrderStatusDistribution() != null) {
                for (Map.Entry<String, Long> entry : data.getOrderStatusDistribution().entrySet()) {
                    Row row = salesSheet.createRow(s1Row++);
                    row.createCell(0).setCellValue(entry.getKey());
                    row.createCell(1).setCellValue(entry.getValue());
                }
            }

            s1Row += 2; // Spacing

            // 3. Top Selling Products
            salesSheet.createRow(s1Row++).createCell(0).setCellValue("TOP SẢN PHẨM BÁN CHẠY");
            Row hpRow = salesSheet.createRow(s1Row++);
            String[] hp = { "Tên sản phẩm", "Số lượng bán" };
            for (int i = 0; i < hp.length; i++) {
                Cell cell = hpRow.createCell(i);
                cell.setCellValue(hp[i]);
                cell.setCellStyle(headerStyle);
            }
            if (data.getTopSellingProducts() != null) {
                for (ResDashboardDTO.ProductStat product : data.getTopSellingProducts()) {
                    Row row = salesSheet.createRow(s1Row++);
                    row.createCell(0).setCellValue(product.getName());
                    row.createCell(1).setCellValue(product.getQuantity());
                }
            }

            s1Row += 2; // Spacing

            // 4. Category Distribution
            salesSheet.createRow(s1Row++).createCell(0).setCellValue("PHÂN BỔ ĐƠN HÀNG THEO DANH MỤC");
            Row hcRow = salesSheet.createRow(s1Row++);
            String[] hc = { "Danh mục", "Số lượng đơn" };
            for (int i = 0; i < hc.length; i++) {
                Cell cell = hcRow.createCell(i);
                cell.setCellValue(hc[i]);
                cell.setCellStyle(headerStyle);
            }
            if (data.getCategoryDistribution() != null) {
                for (ResDashboardDTO.CategoryStat cat : data.getCategoryDistribution()) {
                    Row row = salesSheet.createRow(s1Row++);
                    row.createCell(0).setCellValue(cat.getCategory());
                    row.createCell(1).setCellValue(cat.getCount());
                }
            }

            salesSheet.autoSizeColumn(0);
            salesSheet.autoSizeColumn(1);

            // ---------------------------------------------------------
            // SHEET 2: QUẢN LÝ KHO
            // ---------------------------------------------------------
            Sheet invSheet = workbook.createSheet("Báo cáo Tồn kho");
            int s2Row = 0;

            // Summary
            Row t2Row = invSheet.createRow(s2Row++);
            t2Row.createCell(0).setCellValue("TỔNG HỢP TÌNH TRẠNG KHO");

            Row h2Row = invSheet.createRow(s2Row++);
            String[] h2 = { "Hạng mục", "Giá trị" };
            for (int i = 0; i < h2.length; i++) {
                Cell cell = h2Row.createCell(i);
                cell.setCellValue(h2[i]);
                cell.setCellStyle(headerStyle);
            }

            if (data.getInventorySummary() != null) {
                addRow(invSheet, s2Row++, "Tổng mặt hàng", (double) data.getInventorySummary().getTotalItems(), null);
                addRow(invSheet, s2Row++, "Sản phẩm sắp hết", (double) data.getInventorySummary().getLowStockCount(),
                        null);
                addRow(invSheet, s2Row++, "Sản phẩm hết hàng", (double) data.getInventorySummary().getOutOfStockCount(),
                        null);
            }

            s2Row += 2; // Spacing

            // Detailed Low Stock List
            invSheet.createRow(s2Row++).createCell(0).setCellValue("DANH SÁCH SẢN PHẨM CẦN NHẬP HÀNG");
            Row hDetRow = invSheet.createRow(s2Row++);
            String[] hDet = { "ID", "Tên sản phẩm", "Tồn kho hiện tại" };
            for (int i = 0; i < hDet.length; i++) {
                Cell cell = hDetRow.createCell(i);
                cell.setCellValue(hDet[i]);
                cell.setCellStyle(headerStyle);
            }

            if (data.getLowStockProducts() != null) {
                for (ResDashboardDTO.LowStockProduct p : data.getLowStockProducts()) {
                    Row row = invSheet.createRow(s2Row++);
                    row.createCell(0).setCellValue(p.getId());
                    row.createCell(1).setCellValue(p.getName());
                    row.createCell(2).setCellValue(p.getStock());
                }
            }

            invSheet.autoSizeColumn(0);
            invSheet.autoSizeColumn(1);
            invSheet.autoSizeColumn(2);

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
