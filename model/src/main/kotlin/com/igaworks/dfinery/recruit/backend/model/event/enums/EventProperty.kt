package com.igaworks.dfinery.recruit.backend.model.event.enums

enum class EventProperty(val key: String) {
    // Session
    SESSION_ID("df_session_id"),
    SESSION_DURATION("df_session_duration"),

    // Login
    LOGIN_METHOD("df_login_method"),

    // Purchase
    ORDER_ID("df_order_id"),
    TOTAL_PURCHASE_AMOUNT("df_total_purchase_amount"),
    PAYMENT_METHOD("df_payment_method"),

    // Product
    PRODUCT_ID("df_product_id"),
    PRODUCT_NAME("df_product_name"),
    PRICE("df_price"),
    QUANTITY("df_quantity"),

    // Search
    SEARCH_KEYWORD("df_search_keyword"),

    // Sign Up
    SIGN_UP_METHOD("df_sign_up_method");
}
