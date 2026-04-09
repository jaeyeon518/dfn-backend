package com.igaworks.dfinery.recruit.backend.app.processor.validation

import com.igaworks.dfinery.recruit.backend.app.processor.model.InvalidIngestionRow
import com.igaworks.dfinery.recruit.backend.app.processor.model.ValidIngestionRow
import com.igaworks.dfinery.recruit.backend.app.processor.model.ValidationOutput
import com.igaworks.dfinery.recruit.backend.model.event.Event
import com.igaworks.dfinery.recruit.backend.model.event.enums.EventName
import com.igaworks.dfinery.recruit.backend.model.event.enums.LoginMethod
import com.igaworks.dfinery.recruit.backend.model.event.enums.PaymentMethod
import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionRequestDTO
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class IngestionValidationService {

    fun validate(request: DataIngestionRequestDTO): ValidationOutput {
        val now = Instant.now()
        val ingestedAt = now.toString()

        val validRows = ArrayList<ValidIngestionRow>(request.events.size)
        val invalidRows = ArrayList<InvalidIngestionRow>()

        request.events.forEach { event ->
            val errors = ArrayList<String>()
            errors += validateCommon(request.common)
            errors += validateEvent(event, now)

            if (errors.isEmpty()) {
                validRows += ValidIngestionRow(
                    serviceId = request.common.serviceId,
                    userId = request.common.userId,
                    deviceId = request.common.deviceId,
                    event = event,
                    ingestedAt = ingestedAt
                )
            } else {
                invalidRows += InvalidIngestionRow(
                    serviceId = request.common.serviceId,
                    userId = request.common.userId,
                    deviceId = request.common.deviceId,
                    event = event,
                    reason = errors.joinToString("; "),
                    ingestedAt = ingestedAt
                )
            }
        }

        return ValidationOutput(validRows = validRows, invalidRows = invalidRows)
    }

    private fun validateCommon(common: DataIngestionRequestDTO.Common): List<String> {
        val errors = ArrayList<String>()

        if (common.serviceId.isBlank()) {
            errors += "common.service_id must not be blank"
        } else if (common.serviceId.length > 50) {
            errors += "common.service_id must be at most 50 characters"
        }

        if (common.userId.isBlank()) {
            errors += "common.user_id must not be blank"
        } else if (!isUuidLike(common.userId)) {
            errors += "common.user_id must be a UUID or compact UUID"
        }

        if (common.deviceId.isBlank()) {
            errors += "common.device_id must not be blank"
        } else if (!isUuidLike(common.deviceId)) {
            errors += "common.device_id must be a UUID or compact UUID"
        }

        return errors
    }

    private fun validateEvent(event: Event, now: Instant): List<String> {
        val errors = ArrayList<String>()

        validateEventLogId(event.eventLogId, errors)

        val eventNameValue = event.eventName.trim()
        if (eventNameValue.isEmpty()) {
            errors += "event_name must not be blank"
            return errors
        }

        val eventName = EventName.from(eventNameValue)
        if (eventName == null) {
            errors += "event_name is not supported"
            return errors
        }

        validateEventDatetime(event.eventDatetime, now, errors)
        errors += validateProperties(eventName, event.eventProperties)

        return errors
    }

    private fun validateEventLogId(value: String, errors: MutableList<String>) {
        if (value.isBlank()) {
            errors += "event_log_id must not be blank"
            return
        }

        val parts = value.split(":", limit = 2)
        if (parts.size != 2) {
            errors += "event_log_id must match {uuid}:{timestamp}"
            return
        }

        if (!isCanonicalUuid(parts[0])) {
            errors += "event_log_id UUID part must be valid"
        }
        if (!TIMESTAMP_REGEX.matches(parts[1])) {
            errors += "event_log_id timestamp part must be numeric"
        }
    }

    private fun validateEventDatetime(value: String, now: Instant, errors: MutableList<String>) {
        if (value.isBlank()) {
            errors += "event_datetime must not be blank"
            return
        }

        val eventInstant = runCatching { Instant.parse(value) }.getOrNull()
        if (eventInstant == null) {
            errors += "event_datetime must be ISO-8601"
            return
        }

        if (eventInstant.isAfter(now)) {
            errors += "event_datetime cannot be in the future"
        }
    }

    private fun validateProperties(eventName: EventName, rawProperties: Map<String, Any>?): List<String> {
        val errors = ArrayList<String>()
        val properties = rawProperties ?: emptyMap()

        when (eventName) {
            EventName.START_SESSION -> {
                requireString(properties, "df_session_id", 100, errors)
            }

            EventName.END_SESSION -> {
                requireString(properties, "df_session_id", 100, errors)
                requireIntegralNumberAtLeast(properties, "df_session_duration", 0, errors)
            }

            EventName.LOGIN -> {
                requireEnumValue(
                    properties = properties,
                    key = "df_login_method",
                    allowedValues = LoginMethod.entries.map { it.method }.toSet(),
                    errors = errors
                )
            }

            EventName.LOGOUT -> {
                if (rawProperties != null) {
                    errors += "df_logout must not include event_properties"
                }
            }

            EventName.PURCHASE -> {
                val orderId = requireString(properties, "df_order_id", null, errors)
                if (orderId != null && !isCanonicalUuid(orderId)) {
                    errors += "df_order_id must be UUID"
                }
                requireNumber(properties, "df_total_purchase_amount", minValue = 0.0, strictGreaterThan = true, errors = errors)
                requireEnumValue(
                    properties = properties,
                    key = "df_payment_method",
                    allowedValues = PaymentMethod.entries.map { it.method }.toSet(),
                    errors = errors
                )
            }

            EventName.VIEW_PRODUCT -> {
                requireString(properties, "df_product_id", 100, errors)
                requireString(properties, "df_product_name", 200, errors)
                requireNumber(properties, "df_price", minValue = 0.0, strictGreaterThan = false, errors = errors)
            }

            EventName.ADD_TO_CART -> {
                requireString(properties, "df_product_id", 100, errors)
                requireString(properties, "df_product_name", 200, errors)
                requireNumber(properties, "df_price", minValue = 0.0, strictGreaterThan = false, errors = errors)
                requireIntegralNumberAtLeast(properties, "df_quantity", 1, errors)
            }

            EventName.SEARCH -> {
                requireString(properties, "df_search_keyword", 500, errors)
            }

            EventName.SIGN_UP -> {
                requireEnumValue(
                    properties = properties,
                    key = "df_sign_up_method",
                    allowedValues = LoginMethod.entries.map { it.method }.toSet(),
                    errors = errors
                )
            }

            EventName.ADD_PAYMENT_INFO -> {
                requireEnumValue(
                    properties = properties,
                    key = "df_payment_method",
                    allowedValues = PaymentMethod.entries.map { it.method }.toSet(),
                    errors = errors
                )
            }
        }

        return errors
    }

    private fun requireString(
        properties: Map<String, Any>,
        key: String,
        maxLength: Int?,
        errors: MutableList<String>
    ): String? {
        val raw = properties[key]
        if (raw == null) {
            errors += "$key is required"
            return null
        }

        val value = raw as? String
        if (value == null) {
            errors += "$key must be a string"
            return null
        }

        if (value.isBlank()) {
            errors += "$key must not be blank"
            return null
        }

        if (maxLength != null && value.length > maxLength) {
            errors += "$key must be at most $maxLength characters"
            return null
        }

        return value
    }

    private fun requireEnumValue(
        properties: Map<String, Any>,
        key: String,
        allowedValues: Set<String>,
        errors: MutableList<String>
    ) {
        val value = requireString(properties, key, null, errors) ?: return
        if (value !in allowedValues) {
            errors += "$key must be one of ${allowedValues.joinToString(", ")}"
        }
    }

    private fun requireNumber(
        properties: Map<String, Any>,
        key: String,
        minValue: Double,
        strictGreaterThan: Boolean,
        errors: MutableList<String>
    ): Double? {
        val raw = properties[key]
        if (raw == null) {
            errors += "$key is required"
            return null
        }

        val number = raw as? Number
        if (number == null) {
            errors += "$key must be a number"
            return null
        }

        val value = number.toDouble()
        if (!value.isFinite()) {
            errors += "$key must be a finite number"
            return null
        }
        val isValid = if (strictGreaterThan) value > minValue else value >= minValue
        if (!isValid) {
            errors += "$key must be ${if (strictGreaterThan) ">" else ">="} $minValue"
            return null
        }

        return value
    }

    private fun requireIntegralNumberAtLeast(
        properties: Map<String, Any>,
        key: String,
        minValue: Long,
        errors: MutableList<String>
    ): Long? {
        val raw = properties[key]
        if (raw == null) {
            errors += "$key is required"
            return null
        }

        val number = raw as? Number
        if (number == null) {
            errors += "$key must be an integer"
            return null
        }

        if (!isIntegralNumber(number)) {
            errors += "$key must be an integer"
            return null
        }

        val value = number.toLong()
        if (value < minValue) {
            errors += "$key must be >= $minValue"
            return null
        }

        return value
    }

    private fun isIntegralNumber(number: Number): Boolean {
        return when (number) {
            is Byte, is Short, is Int, is Long -> true
            is Float -> number % 1.0f == 0.0f
            is Double -> number % 1.0 == 0.0
            else -> {
                val asDouble = number.toDouble()
                asDouble.isFinite() && asDouble % 1.0 == 0.0
            }
        }
    }

    private fun isUuidLike(value: String): Boolean {
        return isCanonicalUuid(value) || COMPACT_UUID_REGEX.matches(value)
    }

    private fun isCanonicalUuid(value: String): Boolean {
        return runCatching { UUID.fromString(value) }.isSuccess
    }

    companion object {
        private val TIMESTAMP_REGEX = Regex("^[0-9]{10,17}$")
        private val COMPACT_UUID_REGEX = Regex("^[0-9a-fA-F]{32}$")
    }
}
