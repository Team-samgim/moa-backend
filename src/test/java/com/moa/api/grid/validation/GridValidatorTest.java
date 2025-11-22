package com.moa.api.grid.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.grid.exception.GridException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * GridValidator 테스트
 */
class GridValidatorTest {

    private GridValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GridValidator(new ObjectMapper());
    }

    @Test
    void validateFilterModel_정상_JSON() {
        // given
        String validJson = """
            {
                "src_ip": {
                    "mode": "checkbox",
                    "values": ["192.168.1.1", "10.0.0.1"]
                }
            }
            """;

        // when & then
        assertThatCode(() -> validator.validateFilterModel(validJson))
                .doesNotThrowAnyException();
    }

    @Test
    void validateFilterModel_null_또는_blank는_허용() {
        // when & then
        assertThatCode(() -> validator.validateFilterModel(null))
                .doesNotThrowAnyException();

        assertThatCode(() -> validator.validateFilterModel(""))
                .doesNotThrowAnyException();

        assertThatCode(() -> validator.validateFilterModel("   "))
                .doesNotThrowAnyException();
    }

    @Test
    void validateFilterModel_잘못된_JSON_형식() {
        // given
        String invalidJson = "{ invalid json }";

        // when & then
        assertThatThrownBy(() -> validator.validateFilterModel(invalidJson))
                .isInstanceOf(GridException.class)
                .hasMessageContaining("FilterModel 파싱 실패");
    }

    @Test
    void validateFilterModel_배열_형식은_에러() {
        // given
        String arrayJson = "[1, 2, 3]";

        // when & then
        assertThatThrownBy(() -> validator.validateFilterModel(arrayJson))
                .isInstanceOf(GridException.class)
                .hasMessageContaining("JSON 객체여야 합니다");
    }

    @Test
    void validateFilterModel_잘못된_mode() {
        // given
        String invalidMode = """
            {
                "field1": {
                    "mode": "invalid_mode"
                }
            }
            """;

        // when & then
        assertThatThrownBy(() -> validator.validateFilterModel(invalidMode))
                .isInstanceOf(GridException.class)
                .hasMessageContaining("checkbox")
                .hasMessageContaining("condition");
    }

    @Test
    void validateBaseSpec_정상_JSON() {
        // given
        String validSpec = """
            {
                "time": {
                    "field": "ts_server_nsec",
                    "fromEpoch": 1000000,
                    "toEpoch": 2000000,
                    "inclusive": true
                }
            }
            """;

        // when & then
        assertThatCode(() -> validator.validateBaseSpec(validSpec))
                .doesNotThrowAnyException();
    }

    @Test
    void validateBaseSpec_null_또는_blank는_허용() {
        // when & then
        assertThatCode(() -> validator.validateBaseSpec(null))
                .doesNotThrowAnyException();
    }

    @Test
    void validateBaseSpec_time_필수_필드_누락() {
        // given
        String missingField = """
            {
                "time": {
                    "field": "ts_server_nsec",
                    "fromEpoch": 1000000
                }
            }
            """;

        // when & then
        assertThatThrownBy(() -> validator.validateBaseSpec(missingField))
                .isInstanceOf(GridException.class)
                .hasMessageContaining("toEpoch");
    }

    @Test
    void validateBaseSpec_fromEpoch가_toEpoch보다_큼() {
        // given
        String invalidRange = """
            {
                "time": {
                    "field": "ts_server_nsec",
                    "fromEpoch": 2000000,
                    "toEpoch": 1000000
                }
            }
            """;

        // when & then
        assertThatThrownBy(() -> validator.validateBaseSpec(invalidRange))
                .isInstanceOf(GridException.class)
                .hasMessageContaining("작거나 같아야");
    }

    @Test
    void validateLayer_정상_레이어() {
        // when & then
        assertThatCode(() -> validator.validateLayer("HTTP_PAGE"))
                .doesNotThrowAnyException();

        assertThatCode(() -> validator.validateLayer("http_page"))
                .doesNotThrowAnyException();

        assertThatCode(() -> validator.validateLayer("ETHERNET"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateLayer_null_또는_blank는_허용() {
        // when & then
        assertThatCode(() -> validator.validateLayer(null))
                .doesNotThrowAnyException();

        assertThatCode(() -> validator.validateLayer(""))
                .doesNotThrowAnyException();
    }

    @Test
    void validateLayer_지원하지_않는_레이어() {
        // when & then
        assertThatThrownBy(() -> validator.validateLayer("invalid_layer"))
                .isInstanceOf(GridException.class)
                .hasMessageContaining("지원하지 않는 레이어");
    }
}