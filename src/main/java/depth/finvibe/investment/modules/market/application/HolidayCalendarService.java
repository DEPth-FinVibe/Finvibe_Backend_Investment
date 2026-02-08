package depth.finvibe.investment.modules.market.application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.market.application.port.out.ChkHolidayClient;
import depth.finvibe.investment.modules.market.application.port.out.TradingDayRepository;
import depth.finvibe.investment.modules.market.domain.HolidayDayInfo;
import depth.finvibe.investment.modules.market.domain.TradingDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayCalendarService {

  private static final DateTimeFormatter BASS_DT_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final long KIS_CALL_DELAY_MS = 300;

  private final TradingDayRepository tradingDayRepository;
  private final ChkHolidayClient chkHolidayClient;

  /**
   * 해당 일 이하 중 가장 최근 개장일을 반환.
   * 해당 연월(및 필요 시 이전 달) 데이터가 없으면 KIS 호출로 적재 후 조회.
   */
  public Optional<LocalDate> getLastTradingDayOnOrBefore(LocalDate date) {
    YearMonth month = YearMonth.from(date);
    if (!tradingDayRepository.existsByYearMonth(month.getYear(), month.getMonthValue())) {
      ensureCalendarForMonth(month);
    }
    Optional<LocalDate> last = tradingDayRepository.findLastOpenDayOnOrBefore(date);
    if (last.isEmpty()) {
      YearMonth prevMonth = month.minusMonths(1);
      ensureCalendarForMonth(prevMonth);
      last = tradingDayRepository.findLastOpenDayOnOrBefore(date);
    }
    return last;
  }

  /**
   * 해당 연월의 휴장일 달력이 DB에 없으면 KIS 국내휴장일조회로 적재.
   */
  public void ensureCalendarForMonth(YearMonth yearMonth) {
    if (tradingDayRepository.existsByYearMonth(yearMonth.getYear(), yearMonth.getMonthValue())) {
      return;
    }
    List<TradingDay> tradingDays = new ArrayList<>();
    int lengthOfMonth = yearMonth.lengthOfMonth();
    for (int day = 1; day <= lengthOfMonth; day++) {
      LocalDate d = yearMonth.atDay(day);
      String bassDt = d.format(BASS_DT_FORMAT);
      try {
        List<HolidayDayInfo> infos = chkHolidayClient.fetchChkHoliday(bassDt);
        for (HolidayDayInfo info : infos) {
          tradingDays.add(TradingDay.of(info.date(), info.openDay()));
        }
        if (day < lengthOfMonth) {
          Thread.sleep(KIS_CALL_DELAY_MS);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("휴장일 조회 중 지연 인터럽트. yearMonth={}, day={}", yearMonth, day, e);
        break;
      } catch (Exception e) {
        log.warn("휴장일 조회 실패. bassDt={}", bassDt, e);
      }
    }
    if (!tradingDays.isEmpty()) {
      tradingDayRepository.saveAll(tradingDays);
      log.info("휴장일 달력 적재 완료. yearMonth={}, count={}", yearMonth, tradingDays.size());
    }
  }
}
