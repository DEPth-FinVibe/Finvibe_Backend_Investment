package depth.finvibe.investment.modules.market.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CategoryTest {

    @Test
    @DisplayName("카테고리 생성 시 이름이 설정된다")
    void createCategory() {
        // given & when
        Category category = Category.builder()
                .name("IT")
                .build();

        // then
        assertThat(category.getName()).isEqualTo("IT");
    }

    @Test
    @DisplayName("카테고리명을 변경한다")
    void changeName() {
        // given
        Category category = Category.builder()
                .name("IT")
                .build();

        // when
        category.changeName("반도체");

        // then
        assertThat(category.getName()).isEqualTo("반도체");
    }

    @Test
    @DisplayName("카테고리명이 null이면 예외가 발생한다")
    void changeNameWithNull() {
        // given
        Category category = Category.builder()
                .name("IT")
                .build();

        // when & then
        assertThatThrownBy(() -> category.changeName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("카테고리명은 필수입니다.");
    }

    @Test
    @DisplayName("카테고리명이 공백이면 예외가 발생한다")
    void changeNameWithBlank() {
        // given
        Category category = Category.builder()
                .name("IT")
                .build();

        // when & then
        assertThatThrownBy(() -> category.changeName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("카테고리명은 필수입니다.");
    }

    @Test
    @DisplayName("특정 이름의 카테고리인지 확인한다")
    void hasName() {
        // given
        Category category = Category.builder()
                .name("IT")
                .build();

        // when & then
        assertThat(category.hasName("IT")).isTrue();
        assertThat(category.hasName("반도체")).isFalse();
    }

}