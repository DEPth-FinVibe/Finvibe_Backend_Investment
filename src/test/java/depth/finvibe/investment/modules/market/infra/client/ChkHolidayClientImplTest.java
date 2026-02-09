package depth.finvibe.investment.modules.market.infra.client;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import depth.finvibe.investment.modules.market.domain.HolidayDayInfo;
import depth.finvibe.investment.modules.market.infra.client.dto.KisDto;
import depth.finvibe.investment.shared.error.DomainException;
import depth.finvibe.investment.shared.error.GlobalErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChkHolidayClientImpl 테스트")
class ChkHolidayClientImplTest {

  @Mock
  private KisApiClient kisApiClient;

  @InjectMocks
  private ChkHolidayClientImpl chkHolidayClient;

  @Test
  @DisplayName("24개씩 여러 페이지를 조회하여 전체 데이터를 반환한다")
  void fetchChkHoliday_페이징_완료() {
    // Given: 2월 28일까지 데이터 (24 + 24 = 48개, 필터링 후 28개)
    when(kisApiClient.fetchChkHoliday("20260201"))
        .thenReturn(create24Items("20260201", "20260224"));
    when(kisApiClient.fetchChkHoliday("20260225"))
        .thenReturn(create24Items("20260225", "20260320")); // 2월 4개 + 3월 20개

    // When
    List<HolidayDayInfo> result = chkHolidayClient.fetchChkHoliday(YearMonth.of(2026, 2));

    // Then
    assertThat(result).hasSize(28); // 2월 데이터만 (1~28일)
    assertThat(result).allMatch(info ->
        YearMonth.from(info.date()).equals(YearMonth.of(2026, 2))
    );
    verify(kisApiClient, times(2)).fetchChkHoliday(anyString());
  }

  @Test
  @DisplayName("해당 월의 모든 날짜를 수집하면 페이징을 중단한다")
  void fetchChkHoliday_완전수집_종료() {
    // Given: 31일 달 (24 + 24 = 48개, 31일 수집 완료)
    when(kisApiClient.fetchChkHoliday("20260301"))
        .thenReturn(create24Items("20260301", "20260324"));
    when(kisApiClient.fetchChkHoliday("20260325"))
        .thenReturn(create24Items("20260325", "20260417")); // 3월 7개 + 4월

    // When
    List<HolidayDayInfo> result = chkHolidayClient.fetchChkHoliday(YearMonth.of(2026, 3));

    // Then
    assertThat(result).hasSize(31); // 3월 전체 (1~31일)
    verify(kisApiClient, times(2)).fetchChkHoliday(anyString());
  }

  @Test
  @DisplayName("다음 달 데이터는 필터링하여 제외한다")
  void fetchChkHoliday_다음달_필터링() {
    // Given
    when(kisApiClient.fetchChkHoliday("20260201"))
        .thenReturn(create24Items("20260201", "20260224"));
    when(kisApiClient.fetchChkHoliday("20260225"))
        .thenReturn(create24Items("20260225", "20260320")); // 2월 4개 + 3월 20개

    // When
    List<HolidayDayInfo> result = chkHolidayClient.fetchChkHoliday(YearMonth.of(2026, 2));

    // Then
    assertThat(result)
        .hasSize(28)
        .allMatch(info -> YearMonth.from(info.date()).equals(YearMonth.of(2026, 2)));
  }

  @Test
  @DisplayName("첫 호출 실패 시 예외를 발생시킨다")
  void fetchChkHoliday_첫호출_실패() {
    // Given
    when(kisApiClient.fetchChkHoliday("20260201"))
        .thenThrow(new DomainException(GlobalErrorCode.CIRCUIT_BREAKER_OPEN));

    // When & Then
    assertThatThrownBy(() -> chkHolidayClient.fetchChkHoliday(YearMonth.of(2026, 2)))
        .isInstanceOf(DomainException.class);
  }

  @Test
  @DisplayName("중간 페이징 호출 실패 시 수집된 데이터를 반환한다")
  void fetchChkHoliday_중간호출_실패_부분반환() {
    // Given
    when(kisApiClient.fetchChkHoliday("20260201"))
        .thenReturn(create24Items("20260201", "20260224"));
    when(kisApiClient.fetchChkHoliday("20260225"))
        .thenThrow(new DomainException(GlobalErrorCode.CIRCUIT_BREAKER_OPEN));

    // When
    List<HolidayDayInfo> result = chkHolidayClient.fetchChkHoliday(YearMonth.of(2026, 2));

    // Then
    assertThat(result).hasSize(24); // 첫 페이지 데이터만
    verify(kisApiClient, times(2)).fetchChkHoliday(anyString());
  }

  @Test
  @DisplayName("빈 응답 반환 시 페이징을 중단한다")
  void fetchChkHoliday_빈응답() {
    // Given
    when(kisApiClient.fetchChkHoliday("20260201"))
        .thenReturn(List.of());

    // When
    List<HolidayDayInfo> result = chkHolidayClient.fetchChkHoliday(YearMonth.of(2026, 2));

    // Then
    assertThat(result).isEmpty();
    verify(kisApiClient, times(1)).fetchChkHoliday(anyString());
  }

  @Test
  @DisplayName("최대 호출 횟수(3회)에 도달하면 페이징을 중단한다")
  void fetchChkHoliday_최대호출횟수_제한() {
    // Given: 3번 호출해도 데이터가 계속 오는 경우
    when(kisApiClient.fetchChkHoliday(anyString()))
        .thenReturn(create24Items("20260201", "20260224"));

    // When
    List<HolidayDayInfo> result = chkHolidayClient.fetchChkHoliday(YearMonth.of(2026, 2));

    // Then
    verify(kisApiClient, times(3)).fetchChkHoliday(anyString());
  }

  // Helper methods

  private List<KisDto.ChkHolidayOutput> create24Items(String startDate, String endDate) {
    List<KisDto.ChkHolidayOutput> items = new ArrayList<>();
    LocalDate start = LocalDate.parse(startDate, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    LocalDate end = LocalDate.parse(endDate, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

    LocalDate current = start;
    while (!current.isAfter(end) && items.size() < 24) {
      items.add(createChkHolidayOutput(current));
      current = current.plusDays(1);
    }

    return items;
  }

  private KisDto.ChkHolidayOutput createChkHolidayOutput(LocalDate date) {
    String bassDt = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    return KisDto.ChkHolidayOutput.builder()
        .bass_dt(bassDt)
        .wday_dvsn_cd(String.valueOf(date.getDayOfWeek().getValue()))
        .bzdy_yn("Y")
        .tr_day_yn("Y")
        .opnd_yn("Y")
        .sttl_day_yn("Y")
        .build();
  }
}
