package depth.finvibe.investment.modules.market.infra.client;

import depth.finvibe.investment.modules.market.dto.StockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class KospiKisFileClient implements KisFileClient {

    private static final String KOSPI_ZIP_URL = "https://new.real.download.dws.co.kr/common/master/kospi_code.mst.zip";
    private static final String KOSPI_MST_NAME = "kospi_code.mst";
    private static final Charset KIS_CHARSET = Charset.forName("MS949"); // cp949
    private static final int PART2_TOTAL_WIDTH = 228;
    private static final int[] PART2_WIDTHS = {
            2, 1, 4, 4, 4,
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 9, 5, 5, 1,
            1, 1, 2, 1, 1,
            1, 2, 2, 2, 3,
            1, 3, 12, 12, 8,
            15, 21, 2, 7, 1,
            1, 1, 1, 1, 9,
            9, 9, 5, 9, 8,
            9, 3, 1, 1, 1
    };

    @Override
    public List<StockDto.RealMarketResponse> fetchStocksInKisFile() {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("kis-kospi-");
            Path zipPath = tempDir.resolve("kospi_code.zip");
            downloadFile(KOSPI_ZIP_URL, zipPath);
            Path mstPath = unzipToFile(zipPath, tempDir, KOSPI_MST_NAME);
            return parseKospiFile(mstPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load KOSPI master file", e);
        } finally {
            if (tempDir != null) {
                deleteRecursively(tempDir);
            }
        }
    }

    private void downloadFile(String url, Path target) throws IOException {
        try (InputStream inputStream = new URL(url).openStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path unzipToFile(Path zipPath, Path targetDir, String targetName) throws IOException {
        Path outPath = targetDir.resolve(targetName);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (targetName.equals(entry.getName())) {
                    Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
                    return outPath;
                }
            }
        }
        throw new IOException("Missing " + targetName + " in zip");
    }

    private List<StockDto.RealMarketResponse> parseKospiFile(Path mstPath) throws IOException {
        List<StockDto.RealMarketResponse> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(mstPath, KIS_CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() <= PART2_TOTAL_WIDTH) {
                    continue;
                }
                int part1End = line.length() - PART2_TOTAL_WIDTH;
                String part1 = line.substring(0, part1End);
                String part2 = line.substring(part1End);

                KospiMasterRow row = parseMasterRow(part1, part2);
                String typeCode = selectNonZeroCode(
                        row.bstpSmalDivCode,
                        row.bstpMedmDivCode,
                        row.bstpLargDivCode
                );

                result.add(StockDto.RealMarketResponse.builder()
                        .symbol(row.mkscShrnIscd)
                        .name(row.htsKorIsnm)
                        .typeCode(typeCode)
                        .build());
            }
        }
        return result;
    }

    private KospiMasterRow parseMasterRow(String part1, String part2) {
        String symbol = rstrip(part1.substring(0, Math.min(9, part1.length())));
        int standardStart = Math.min(9, part1.length());
        int standardEnd = Math.min(21, part1.length());
        String standard = rstrip(part1.substring(standardStart, standardEnd));
        String name = rstrip(part1.substring(Math.min(21, part1.length())));
        String[] fields = splitFixedWidth(part2, PART2_WIDTHS);
        return new KospiMasterRow(symbol, standard, name, fields);
    }

    private String[] splitFixedWidth(String value, int[] widths) {
        String[] fields = new String[widths.length];
        int offset = 0;
        for (int i = 0; i < widths.length; i++) {
            int end = Math.min(value.length(), offset + widths[i]);
            if (offset >= value.length()) {
                fields[i] = "";
            } else {
                fields[i] = value.substring(offset, end).trim();
            }
            offset += widths[i];
        }
        return fields;
    }

    private String selectNonZeroCode(String... codes) {
        for (String code : codes) {
            if (code != null && !code.isBlank() && !"0000".equals(code)) {
                return code;
            }
        }

        log.warn("All codes are zero or blank: {}", String.join(", ", codes));
        return "";
    }

    private String rstrip(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static class KospiMasterRow {
        private final String mkscShrnIscd;
        private final String stndIscd;
        private final String htsKorIsnm;
        private final String scrtGrpClsCode;
        private final String avlsScalClsCode;
        private final String bstpLargDivCode;
        private final String bstpMedmDivCode;
        private final String bstpSmalDivCode;
        private final String mninClsCodeYn;
        private final String lowCurrentYn;
        private final String sprnStrrNmixIssuYn;
        private final String kospi200ApntClsCode;
        private final String kospi100IssuYn;
        private final String kospi50IssuYn;
        private final String krxIssuYn;
        private final String etpProdClsCode;
        private final String elwPblcYn;
        private final String krx100IssuYn;
        private final String krxCarYn;
        private final String krxSmcnYn;
        private final String krxBioYn;
        private final String krxBankYn;
        private final String etprUndtObjtCoYn;
        private final String krxEnrgChmsYn;
        private final String krxStelYn;
        private final String shortOverClsCode;
        private final String krxMediCmncYn;
        private final String krxCnstYn;
        private final String krxFnncSvcYn;
        private final String krxScrtYn;
        private final String krxShipYn;
        private final String krxInsuYn;
        private final String krxTrnpYn;
        private final String sriNmixYn;
        private final String stckSdpr;
        private final String frmlMrktDealQtyUnit;
        private final String ovtmMrktDealQtyUnit;
        private final String trhtYn;
        private final String sltrYn;
        private final String mangIssuYn;
        private final String mrktAlrmClsCode;
        private final String mrktAlrmRiskAdntYn;
        private final String insnPbntYn;
        private final String bypsLstnYn;
        private final String flngClsCode;
        private final String fcamModClsCode;
        private final String icicClsCode;
        private final String margRate;
        private final String crdtAble;
        private final String crdtDays;
        private final String prdyVol;
        private final String stckFcam;
        private final String stckLstnDate;
        private final String lstnStcn;
        private final String cpfn;
        private final String stacMonth;
        private final String poPrc;
        private final String prstClsCode;
        private final String sstsHotYn;
        private final String stangeRunupYn;
        private final String krx300IssuYn;
        private final String kospiIssuYn;
        private final String saleAccount;
        private final String bsopPrfi;
        private final String opPrfi;
        private final String thtrNtin;
        private final String roe;
        private final String baseDate;
        private final String prdyAvlsScal;
        private final String grpCode;
        private final String coCrdtLimtOverYn;
        private final String secuLendAbleYn;
        private final String stlnAbleYn;

        private KospiMasterRow(String mkscShrnIscd, String stndIscd, String htsKorIsnm, String[] part2) {
            this.mkscShrnIscd = mkscShrnIscd;
            this.stndIscd = stndIscd;
            this.htsKorIsnm = htsKorIsnm;
            int idx = 0;
            this.scrtGrpClsCode = part2[idx++];
            this.avlsScalClsCode = part2[idx++];
            this.bstpLargDivCode = part2[idx++];
            this.bstpMedmDivCode = part2[idx++];
            this.bstpSmalDivCode = part2[idx++];
            this.mninClsCodeYn = part2[idx++];
            this.lowCurrentYn = part2[idx++];
            this.sprnStrrNmixIssuYn = part2[idx++];
            this.kospi200ApntClsCode = part2[idx++];
            this.kospi100IssuYn = part2[idx++];
            this.kospi50IssuYn = part2[idx++];
            this.krxIssuYn = part2[idx++];
            this.etpProdClsCode = part2[idx++];
            this.elwPblcYn = part2[idx++];
            this.krx100IssuYn = part2[idx++];
            this.krxCarYn = part2[idx++];
            this.krxSmcnYn = part2[idx++];
            this.krxBioYn = part2[idx++];
            this.krxBankYn = part2[idx++];
            this.etprUndtObjtCoYn = part2[idx++];
            this.krxEnrgChmsYn = part2[idx++];
            this.krxStelYn = part2[idx++];
            this.shortOverClsCode = part2[idx++];
            this.krxMediCmncYn = part2[idx++];
            this.krxCnstYn = part2[idx++];
            this.krxFnncSvcYn = part2[idx++];
            this.krxScrtYn = part2[idx++];
            this.krxShipYn = part2[idx++];
            this.krxInsuYn = part2[idx++];
            this.krxTrnpYn = part2[idx++];
            this.sriNmixYn = part2[idx++];
            this.stckSdpr = part2[idx++];
            this.frmlMrktDealQtyUnit = part2[idx++];
            this.ovtmMrktDealQtyUnit = part2[idx++];
            this.trhtYn = part2[idx++];
            this.sltrYn = part2[idx++];
            this.mangIssuYn = part2[idx++];
            this.mrktAlrmClsCode = part2[idx++];
            this.mrktAlrmRiskAdntYn = part2[idx++];
            this.insnPbntYn = part2[idx++];
            this.bypsLstnYn = part2[idx++];
            this.flngClsCode = part2[idx++];
            this.fcamModClsCode = part2[idx++];
            this.icicClsCode = part2[idx++];
            this.margRate = part2[idx++];
            this.crdtAble = part2[idx++];
            this.crdtDays = part2[idx++];
            this.prdyVol = part2[idx++];
            this.stckFcam = part2[idx++];
            this.stckLstnDate = part2[idx++];
            this.lstnStcn = part2[idx++];
            this.cpfn = part2[idx++];
            this.stacMonth = part2[idx++];
            this.poPrc = part2[idx++];
            this.prstClsCode = part2[idx++];
            this.sstsHotYn = part2[idx++];
            this.stangeRunupYn = part2[idx++];
            this.krx300IssuYn = part2[idx++];
            this.kospiIssuYn = part2[idx++];
            this.saleAccount = part2[idx++];
            this.bsopPrfi = part2[idx++];
            this.opPrfi = part2[idx++];
            this.thtrNtin = part2[idx++];
            this.roe = part2[idx++];
            this.baseDate = part2[idx++];
            this.prdyAvlsScal = part2[idx++];
            this.grpCode = part2[idx++];
            this.coCrdtLimtOverYn = part2[idx++];
            this.secuLendAbleYn = part2[idx++];
            this.stlnAbleYn = part2[idx];
        }
    }

    private void deleteRecursively(Path root) {
        try {
            if (!Files.exists(root)) {
                return;
            }
            Files.walk(root)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // best-effort cleanup
                        }
                    });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
