package depth.finvibe.investment.modules.market.infra.client;

import depth.finvibe.investment.modules.market.dto.StockDto;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KospiKisFileClientTest {

    private static final Charset KIS_CHARSET = Charset.forName("MS949");
    private static final int PART2_TOTAL_WIDTH = 228;
    private static final int BSTP_MEDM_DIV_START = 7;
    private static final int BSTP_MEDM_DIV_LENGTH = 4;

    @Test
    void parseKospiFile_parsesSymbolNameAndTypeCode() throws Exception {
        KospiKisFileClient client = new KospiKisFileClient();
        Path mstFile = Files.createTempFile("kospi", ".mst");
        try {
            String validLine = buildLine("005930", "SAMSUNG ELEC", "1234");
            String shortLine = "short";
            Files.write(mstFile, List.of(shortLine, validLine), KIS_CHARSET);

            List<StockDto.RealMarketResponse> result = invokeParse(client, mstFile);

            assertThat(result).hasSize(1);
            StockDto.RealMarketResponse parsed = result.get(0);
            assertThat(parsed.getSymbol()).isEqualTo("005930");
            assertThat(parsed.getName()).isEqualTo("SAMSUNG ELEC");
            assertThat(parsed.getTypeCode()).isEqualTo("1234");
        } finally {
            Files.deleteIfExists(mstFile);
        }
    }

    @Test
    void extractBstpMedmDivCode_returnsEmptyForShortInput() throws Exception {
        KospiKisFileClient client = new KospiKisFileClient();
        Method method = KospiKisFileClient.class.getDeclaredMethod("extractBstpMedmDivCode", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(client, "short");

        assertThat(result).isEmpty();
    }

    private List<StockDto.RealMarketResponse> invokeParse(KospiKisFileClient client, Path mstFile) throws Exception {
        Method method = KospiKisFileClient.class.getDeclaredMethod("parseKospiFile", Path.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<StockDto.RealMarketResponse> result =
                (List<StockDto.RealMarketResponse>) method.invoke(client, mstFile);
        return result;
    }

    private String buildLine(String symbol, String name, String typeCode) {
        int part1Length = 40;
        String part1 = padRight(symbol, 9)
                + " ".repeat(12)
                + padRight(name, part1Length - 21);
        String part2 = buildPart2(typeCode);
        return part1 + part2;
    }

    private String buildPart2(String typeCode) {
        char[] chars = new char[PART2_TOTAL_WIDTH];
        Arrays.fill(chars, ' ');
        String padded = padRight(typeCode, BSTP_MEDM_DIV_LENGTH);
        for (int i = 0; i < BSTP_MEDM_DIV_LENGTH; i++) {
            chars[BSTP_MEDM_DIV_START + i] = padded.charAt(i);
        }
        return new String(chars);
    }

    private String padRight(String value, int length) {
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }
}
