package com.igaworks.dfinery.recruit.backend.app.processor.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.igaworks.dfinery.recruit.backend.app.processor.model.InvalidIngestionRow
import com.igaworks.dfinery.recruit.backend.app.processor.model.ValidIngestionRow
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

@Component
class ParquetBatchWriter(
    private val objectMapper: ObjectMapper,
    @Value("\${processor.storage.base-dir:./processor-output}") baseDir: String,
    @Value("\${processor.storage.batch-size:2000}")
    private val batchSize: Int
) {
    private val validBuffer = ArrayList<ValidIngestionRow>(batchSize)
    private val invalidBuffer = ArrayList<InvalidIngestionRow>(batchSize)

    private val validSeq = AtomicInteger(0)
    private val invalidSeq = AtomicInteger(0)

    private val outputDir: Path = Paths.get(baseDir)

    init {
        Files.createDirectories(outputDir)
    }

    @Synchronized
    fun append(validRows: List<ValidIngestionRow>, invalidRows: List<InvalidIngestionRow>) {
        if (validRows.isNotEmpty()) {
            validBuffer.addAll(validRows)
            flushIfNeeded(isValid = true)
        }
        if (invalidRows.isNotEmpty()) {
            invalidBuffer.addAll(invalidRows)
            flushIfNeeded(isValid = false)
        }
    }

    @Synchronized
    fun flushAll() {
        flushRemaining(isValid = true)
        flushRemaining(isValid = false)
    }

    private fun flushIfNeeded(isValid: Boolean) {
        val target = if (isValid) validBuffer else invalidBuffer
        while (target.size >= batchSize) {
            val chunk = target.take(batchSize)
            if (isValid) {
                writeValidChunk(chunk.filterIsInstance<ValidIngestionRow>())
            } else {
                writeInvalidChunk(chunk.filterIsInstance<InvalidIngestionRow>())
            }
            target.subList(0, batchSize).clear()
        }
    }

    private fun flushRemaining(isValid: Boolean) {
        val target = if (isValid) validBuffer else invalidBuffer
        if (target.isEmpty()) return

        if (isValid) {
            writeValidChunk(target.filterIsInstance<ValidIngestionRow>())
        } else {
            writeInvalidChunk(target.filterIsInstance<InvalidIngestionRow>())
        }
        target.clear()
    }

    private fun writeValidChunk(rows: List<ValidIngestionRow>) {
        val path = nextOutputPath(prefix = "valid", seq = validSeq.incrementAndGet())
        writeRecords(path, VALID_SCHEMA, rows.map(::toValidRecord))
    }

    private fun writeInvalidChunk(rows: List<InvalidIngestionRow>) {
        val path = nextOutputPath(prefix = "invalid", seq = invalidSeq.incrementAndGet())
        writeRecords(path, INVALID_SCHEMA, rows.map(::toInvalidRecord))
    }

    private fun writeRecords(path: Path, schema: Schema, records: List<GenericRecord>) {
        val outputFile = NioParquetFiles.outputFile(path)
        AvroParquetWriter.builder<GenericRecord>(outputFile)
            .withSchema(schema)
            .withCompressionCodec(CompressionCodecName.UNCOMPRESSED)
            .build()
            .use { writer ->
                records.forEach { record ->
                    writer.write(record)
                }
            }
    }

    private fun nextOutputPath(prefix: String, seq: Int): Path {
        val timestamp = FILE_TS_FORMAT.format(Instant.now())
        return outputDir.resolve("$prefix-$timestamp-$seq.parquet")
    }

    private fun toValidRecord(row: ValidIngestionRow): GenericRecord {
        return GenericData.Record(VALID_SCHEMA).apply {
            put("service_id", row.serviceId)
            put("user_id", row.userId)
            put("device_id", row.deviceId)
            put("event_log_id", row.event.eventLogId)
            put("event_name", row.event.eventName)
            put("event_datetime", row.event.eventDatetime)
            put("event_properties_json", eventPropertiesJson(row.event.eventProperties))
            put("ingested_at", row.ingestedAt)
        }
    }

    private fun toInvalidRecord(row: InvalidIngestionRow): GenericRecord {
        return GenericData.Record(INVALID_SCHEMA).apply {
            put("service_id", row.serviceId)
            put("user_id", row.userId)
            put("device_id", row.deviceId)
            put("event_log_id", row.event.eventLogId)
            put("event_name", row.event.eventName)
            put("event_datetime", row.event.eventDatetime)
            put("event_properties_json", eventPropertiesJson(row.event.eventProperties))
            put("reason", row.reason)
            put("ingested_at", row.ingestedAt)
        }
    }

    private fun eventPropertiesJson(properties: Map<String, Any>?): String? {
        return properties?.let(objectMapper::writeValueAsString)
    }

    companion object {
        private val FILE_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC)

        private val NULLABLE_STRING = Schema.createUnion(
            listOf(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING))
        )

        private val VALID_SCHEMA: Schema = SchemaBuilder.record("valid_ingestion_row")
            .namespace("com.igaworks.dfinery.recruit.backend.app.processor.storage")
            .fields()
            .requiredString("service_id")
            .requiredString("user_id")
            .requiredString("device_id")
            .requiredString("event_log_id")
            .requiredString("event_name")
            .requiredString("event_datetime")
            .name("event_properties_json").type(NULLABLE_STRING).withDefault(null)
            .requiredString("ingested_at")
            .endRecord()

        private val INVALID_SCHEMA: Schema = SchemaBuilder.record("invalid_ingestion_row")
            .namespace("com.igaworks.dfinery.recruit.backend.app.processor.storage")
            .fields()
            .requiredString("service_id")
            .requiredString("user_id")
            .requiredString("device_id")
            .requiredString("event_log_id")
            .requiredString("event_name")
            .requiredString("event_datetime")
            .name("event_properties_json").type(NULLABLE_STRING).withDefault(null)
            .requiredString("reason")
            .requiredString("ingested_at")
            .endRecord()
    }
}
