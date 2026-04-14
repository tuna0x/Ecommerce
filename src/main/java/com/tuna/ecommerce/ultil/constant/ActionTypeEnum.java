package com.tuna.ecommerce.ultil.constant;

public enum ActionTypeEnum {
    // Sản phẩm
    VIEW_PRODUCT,       // Xem chi tiết sản phẩm
    CLICK_PRODUCT,      // Click card sản phẩm từ danh sách

    // Giỏ hàng
    ADD_CART,            // Thêm vào giỏ
    REMOVE_CART,         // Xóa khỏi giỏ
    UPDATE_CART,         // Thay đổi số lượng

    // Mua hàng
    PURCHASE,            // Hoàn tất thanh toán
    BEGIN_CHECKOUT,      // Bắt đầu checkout

    // Tìm kiếm & Duyệt
    SEARCH,              // Tìm kiếm keyword
    VIEW_CATEGORY,       // Xem danh mục

    // Engagement
    TIME_ON_PAGE,        // Thời gian ở trên trang

    // Tương tác
    USE_COUPON,          // Áp dụng mã giảm giá
    CHAT_WITH_BOT        // Tương tác chatbot
}
