package io.github.ingkoon.artinus.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI artinusOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ARTINUS 구독 서비스 API")
                        .version("v1")
                        .description("""
                                회원이 여러 채널(홈페이지·네이버·콜센터 등)을 통해 구독/해지를 수행하는 백엔드 API.
                                각 행위는 외부 API(csrng) 응답에 따라 커밋/롤백되며, 구독 이력은 불변 로그로 남고
                                이력 조회 시 LLM(Claude)이 자연어 요약을 생성한다.

                                - 상태(UserStatus): NONE(구독 안함) / BASIC(일반 구독) / PREMIUM(프리미엄 구독)
                                - 구독 전이: NONE→{BASIC,PREMIUM}, BASIC→{PREMIUM}
                                - 해지 전이: PREMIUM→{BASIC,NONE}, BASIC→{NONE}
                                """)
                        .contact(new Contact().name("ingkoon")));
    }
}
