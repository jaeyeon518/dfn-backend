package com.igaworks.dfinery.recruit.backend.model.event.enums

enum class EventName(val eventName: String) {
    START_SESSION("df_start_session"),
    END_SESSION("df_end_session"),
    LOGIN("df_login"),
    LOGOUT("df_logout"),
    PURCHASE("df_purchase"),
    VIEW_PRODUCT("df_view_product"),
    ADD_TO_CART("df_add_to_cart"),
    SEARCH("df_search"),
    SIGN_UP("df_sign_up"),
    ADD_PAYMENT_INFO("df_add_payment_info");

    companion object {
        fun from(value: String): EventName? = entries.find { it.eventName == value }
    }
}
