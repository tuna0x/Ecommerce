package com.tuna.ecommerce.ultil.constant;

public enum InventoryLogType {
    PURCHASE,    // Nhập hàng mới
    SALE,        // Bán hàng cho khách
    ADJUSTMENT,  // Điều chỉnh thủ công (kiểm kho)
    RETURN,      // Khách trả hàng
    DAMAGE,      // Hàng lỗi/hư hỏng
    LOSS,        // Thất thoát
    RESERVE,     // Giữ hàng khi khách đặt
    RELEASE      // Giải phóng hàng khi hủy đơn
}
