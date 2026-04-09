package com.igaworks.dfinery.recruit.backend.app.ingestion.controller

import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionRequestDTO
import com.igaworks.dfinery.recruit.backend.model.ingestion.DataIngestionResponseDTO
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class DataIngestionController {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 데이터 수집 API
     *
     * 이 API를 통해 수신된 데이터를 비동기로 처리하는 파이프라인을 구현하세요.
     *
     * [Required]
     * - 수신된 데이터를 비동기 처리 파이프라인으로 전달
     * - 데이터 규칙에 의거한 Validation 처리
     * - Validation 실패 데이터에 대한 처리 설계
     * - 최대 2,000개 row 단위로 JSON 파일 로컬 적재
     *
     * [Optional]
     * - 병렬 처리
     * - Backpressure 처리
     * - 처리량 최적화
     */
    @PostMapping("/v1/collect")
    suspend fun collect(@RequestBody request: DataIngestionRequestDTO): DataIngestionResponseDTO {
        log.info(
            "Received collect request: serviceId={}, userId={}, deviceId={}, eventCount={}",
            request.common.serviceId,
            request.common.userId,
            request.common.deviceId,
            request.events.size
        )

        // TODO: 비동기 처리 파이프라인으로 전달하는 로직을 구현하세요.

        return DataIngestionResponseDTO(success = true, rowCount = request.events.size)
    }
}
