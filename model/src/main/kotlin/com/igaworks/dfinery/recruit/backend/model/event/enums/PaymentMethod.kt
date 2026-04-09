package com.igaworks.dfinery.recruit.backend.model.event.enums

enum class PaymentMethod(val method: String) {
    CARD("Card"),
    BANK_TRANSFER("BankTransfer"),
    CASH("Cash");
}
