package com.igaworks.dfinery.recruit.backend.library.generator

import com.igaworks.dfinery.recruit.backend.model.event.Event
import com.igaworks.dfinery.recruit.backend.model.event.enums.EventName
import com.igaworks.dfinery.recruit.backend.model.event.enums.EventProperty
import com.igaworks.dfinery.recruit.backend.model.event.enums.LoginMethod
import com.igaworks.dfinery.recruit.backend.model.event.enums.PaymentMethod
import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionRequestDTO
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

object EventDataGenerator {

    fun generateRequest(eventsPerRequest: Int): DataIngestionRequestDTO {
        val common = DataIngestionRequestDTO.Common(
            serviceId = "test-svc",
            userId = UUID.randomUUID().toString(),
            deviceId = UUID.randomUUID().toString()
        )
        val events = (1..eventsPerRequest).map { generateEvent() }
        return DataIngestionRequestDTO(common = common, events = events)
    }

    private fun generateEvent(): Event {
        val name = EventName.entries.random()
        return Event(
            eventLogId = "${UUID.randomUUID()}:${Instant.now().toEpochMilli()}",
            eventName = name.eventName,
            eventDatetime = Instant.now().toString(),
            eventProperties = generateProperties(name)
        )
    }

    private fun generateProperties(eventName: EventName): Map<String, Any>? {
        return when (eventName) {
            EventName.START_SESSION -> mapOf(
                EventProperty.SESSION_ID.key to "${UUID.randomUUID()}:${Instant.now().toEpochMilli()}"
            )
            EventName.END_SESSION -> mapOf(
                EventProperty.SESSION_ID.key to "${UUID.randomUUID()}:${Instant.now().toEpochMilli()}",
                EventProperty.SESSION_DURATION.key to Random.nextInt(1, 3600)
            )
            EventName.LOGIN -> mapOf(
                EventProperty.LOGIN_METHOD.key to LoginMethod.entries.random().method
            )
            EventName.LOGOUT -> null
            EventName.PURCHASE -> mapOf(
                EventProperty.ORDER_ID.key to UUID.randomUUID().toString(),
                EventProperty.TOTAL_PURCHASE_AMOUNT.key to Random.nextDouble(1000.0, 1000000.0),
                EventProperty.PAYMENT_METHOD.key to PaymentMethod.entries.random().method
            )
            EventName.VIEW_PRODUCT -> mapOf(
                EventProperty.PRODUCT_ID.key to "product_${Random.nextInt(1, 1000)}",
                EventProperty.PRODUCT_NAME.key to "item_${randomString(5)}",
                EventProperty.PRICE.key to Random.nextDouble(1000.0, 500000.0)
            )
            EventName.ADD_TO_CART -> mapOf(
                EventProperty.PRODUCT_ID.key to "product_${Random.nextInt(1, 1000)}",
                EventProperty.PRODUCT_NAME.key to "item_${randomString(5)}",
                EventProperty.PRICE.key to Random.nextDouble(1000.0, 500000.0),
                EventProperty.QUANTITY.key to Random.nextInt(1, 10)
            )
            EventName.SEARCH -> mapOf(
                EventProperty.SEARCH_KEYWORD.key to randomString(Random.nextInt(2, 20))
            )
            EventName.SIGN_UP -> mapOf(
                EventProperty.SIGN_UP_METHOD.key to LoginMethod.entries.random().method
            )
            EventName.ADD_PAYMENT_INFO -> mapOf(
                EventProperty.PAYMENT_METHOD.key to PaymentMethod.entries.random().method
            )
        }
    }

    private fun randomString(length: Int): String {
        val chars = ('a'..'z') + ('0'..'9')
        return (1..length).map { chars.random() }.joinToString("")
    }
}
