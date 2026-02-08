package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.HolidayDayInfo;

import java.util.List;

/**
 * KIS 국내휴장일조회(chk_holiday) API 호출 포트.
 */
public interface ChkHolidayClient {

  /**
   * 기준일자의 개장일 여부를 조회. 보통 1건 반환.
   *
   * @param bassDt 기준일자 (YYYYMMDD)
   * @return 해당 일자의 휴장일 정보 (date, openDay)
   */
  List<HolidayDayInfo> fetchChkHoliday(String bassDt);
}
