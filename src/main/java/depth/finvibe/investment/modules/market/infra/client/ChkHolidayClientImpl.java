package depth.finvibe.investment.modules.market.infra.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.application.port.out.ChkHolidayClient;
import depth.finvibe.investment.modules.market.domain.HolidayDayInfo;
import depth.finvibe.investment.modules.market.infra.client.dto.KisDto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChkHolidayClientImpl implements ChkHolidayClient {

  private static final DateTimeFormatter BASS_DT_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final KisApiClient kisApiClient;

  @Override
  public List<HolidayDayInfo> fetchChkHoliday(String bassDt) {
    List<KisDto.ChkHolidayOutput> output = kisApiClient.fetchChkHoliday(bassDt);
    return output.stream()
        .map(this::toHolidayDayInfo)
        .toList();
  }

  private HolidayDayInfo toHolidayDayInfo(KisDto.ChkHolidayOutput o) {
    LocalDate date = LocalDate.parse(o.getBass_dt(), BASS_DT_FORMAT);
    boolean openDay = "Y".equalsIgnoreCase(o.getOpnd_yn());
    return new HolidayDayInfo(date, openDay);
  }
}
