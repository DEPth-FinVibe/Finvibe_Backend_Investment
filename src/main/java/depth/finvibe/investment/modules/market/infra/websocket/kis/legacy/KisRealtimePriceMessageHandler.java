package depth.finvibe.investment.modules.market.infra.websocket.kis.legacy;

import depth.finvibe.investment.modules.market.application.port.in.CurrentPriceCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisRealtimePriceMessageHandler {
    private static final List<String> COLUMNS = List.of(
            "MKSC_SHRN_ISCD", "STCK_CNTG_HOUR", "STCK_PRPR", "PRDY_VRSS_SIGN",
            "PRDY_VRSS", "PRDY_CTRT", "WGHN_AVRG_STCK_PRC", "STCK_OPRC",
            "STCK_HGPR", "STCK_LWPR", "ASKP1", "BIDP1", "CNTG_VOL", "ACML_VOL",
            "ACML_TR_PBMN", "SELN_CNTG_CSNU", "SHNU_CNTG_CSNU", "NTBY_CNTG_CSNU",
            "CTTR", "SELN_CNTG_SMTN", "SHNU_CNTG_SMTN", "CCLD_DVSN", "SHNU_RATE",
            "PRDY_VOL_VRSS_ACML_VOL_RATE", "OPRC_HOUR", "OPRC_VRSS_PRPR_SIGN",
            "OPRC_VRSS_PRPR", "HGPR_HOUR", "HGPR_VRSS_PRPR_SIGN", "HGPR_VRSS_PRPR",
            "LWPR_HOUR", "LWPR_VRSS_PRPR_SIGN", "LWPR_VRSS_PRPR", "BSOP_DATE",
            "NEW_MKOP_CLS_CODE", "TRHT_YN", "ASKP_RSQN1", "BIDP_RSQN1",
            "TOTAL_ASKP_RSQN", "TOTAL_BIDP_RSQN", "VOL_TNRT",
            "PRDY_SMNS_HOUR_ACML_VOL", "PRDY_SMNS_HOUR_ACML_VOL_RATE",
            "HOUR_CLS_CODE", "MRKT_TRTM_CLS_CODE", "VI_STND_PRC"
    );

    private static final ZoneId KIS_ZONE = ZoneId.of("Asia/Seoul");

    private final CurrentPriceCommandUseCase currentPriceCommandUseCase;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, AesKeyIv> encryptionKeys = new ConcurrentHashMap<>();

    public void handleMessage(String message, Function<String, Long> stockIdResolver) {
        if (message == null || message.isBlank()) {
            return;
        }
        String trimmed = message.trim();
        if (trimmed.startsWith("{")) {
            handleJsonMessage(trimmed);
            return;
        }
        handleDataMessage(trimmed, stockIdResolver);
    }

    private void handleJsonMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode body = root.path("body");
            String msg1 = body.path("msg1").asText("");
            if (!msg1.contains("SUBSCRIBE")) {
                return;
            }
            String trId = root.path("header").path("tr_id").asText("");
            JsonNode output = body.path("output");
            String iv = output.path("iv").asText("");
            String key = output.path("key").asText("");
            if (!trId.isBlank() && !iv.isBlank() && !key.isBlank()) {
                encryptionKeys.put(trId, new AesKeyIv(key, iv));
            }
        } catch (Exception ex) {
            log.warn("Failed to parse websocket json message.", ex);
        }
    }

    private void handleDataMessage(String message, Function<String, Long> stockIdResolver) {
        String[] parts = message.split("\\|", 4);
        if (parts.length < 4) {
            return;
        }
        String encryptedFlag = parts[0];
        String trId = parts[1];
        String payload = parts[3];

        String data = payload;
        if ("1".equals(encryptedFlag)) {
            AesKeyIv keyIv = encryptionKeys.get(trId);
            if (keyIv == null) {
                log.warn("Missing encryption key for tr_id={}", trId);
                return;
            }
            data = decrypt(payload, keyIv);
            if (data == null) {
                return;
            }
        }

        String[] values = data.split("\\^");
        int columnSize = COLUMNS.size();
        for (int i = 0; i + columnSize <= values.length; i += columnSize) {
            Map<String, String> record = toRecord(values, i, columnSize);
            processRecord(record, stockIdResolver);
        }
    }

    private Map<String, String> toRecord(String[] values, int start, int columnSize) {
        Map<String, String> record = new java.util.HashMap<>();
        for (int i = 0; i < columnSize; i++) {
            record.put(COLUMNS.get(i), values[start + i]);
        }
        return record;
    }

    private void processRecord(Map<String, String> record, Function<String, Long> stockIdResolver) {
        String symbol = record.get("MKSC_SHRN_ISCD");
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        Long stockId = stockIdResolver.apply(symbol);
        if (stockId == null) {
            stockId = stockRepository.findBySymbol(symbol).map(Stock::getId).orElse(null);
        }
        if (stockId == null) {
            return;
        }

        CurrentPriceUpdatedEvent event = CurrentPriceUpdatedEvent.builder()
                .stockId(stockId)
                .timeframe(Timeframe.MINUTE)
                .at(parseDateTime(record.get("BSOP_DATE"), record.get("STCK_CNTG_HOUR")))
                .open(parseBigDecimal(record.get("STCK_OPRC")))
                .high(parseBigDecimal(record.get("STCK_HGPR")))
                .low(parseBigDecimal(record.get("STCK_LWPR")))
                .close(parseBigDecimal(record.get("STCK_PRPR")))
                .prevDayChangePct(parseBigDecimal(record.get("PRDY_CTRT")))
                .volume(parseBigDecimal(record.get("ACML_VOL")))
                .value(parseBigDecimal(record.get("ACML_TR_PBMN")))
                .build();

        currentPriceCommandUseCase.stockPriceUpdated(event);
    }

    private LocalDateTime parseDateTime(String date, String time) {
        LocalDate parsedDate;
        if (date == null || date.isBlank()) {
            parsedDate = LocalDate.now(KIS_ZONE);
        } else {
            parsedDate = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
        }

        if (time == null || time.isBlank()) {
            return parsedDate.atStartOfDay();
        }
        String normalizedTime = time.length() == 4 ? time + "00" : time;
        LocalTime parsedTime = LocalTime.parse(normalizedTime, DateTimeFormatter.ofPattern("HHmmss"));
        return LocalDateTime.of(parsedDate, parsedTime);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = value.replace(",", "");
        return new BigDecimal(normalized);
    }

    private String decrypt(String payload, AesKeyIv keyIv) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(payload);
            SecretKeySpec keySpec = new SecretKeySpec(keyIv.key().getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(keyIv.iv().getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("Failed to decrypt websocket payload.", ex);
            return null;
        }
    }

    private record AesKeyIv(String key, String iv) {}
}
