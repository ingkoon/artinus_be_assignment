package io.github.ingkoon.artinus.user.controller;

import io.github.ingkoon.artinus.common.exception.ErrorResponse;
import io.github.ingkoon.artinus.user.controller.request.CancelRequest;
import io.github.ingkoon.artinus.user.controller.request.SubscribeRequest;
import io.github.ingkoon.artinus.user.service.result.ReadHistorySummaryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 구독 API의 라우팅 · 검증 · OpenAPI(Swagger) 문서 정의.
 * 웹/검증/문서 어노테이션을 인터페이스로 모아 컨트롤러는 순수 위임만 담당한다.
 * (Spring 6.1+는 인터페이스의 파라미터 어노테이션을 구현체와 병합한다. 구현체가 제약을
 *  덧붙이지 않으므로 Bean Validation의 Liskov 규칙(HV000151)도 위반하지 않는다.)
 */
@Tag(name = "구독 API", description = "구독/해지 및 이력 조회(LLM 요약)")
public interface UserApi {

    @Operation(summary = "구독 이력 조회 + LLM 요약",
            description = "회원의 구독/해지 이력을 시간순으로 조회하고, Claude가 생성한 자연어 요약을 함께 반환한다. "
                    + "요약 생성에 실패해도 목록은 정상 반환한다(graceful degradation).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "phone 파라미터 누락/공백",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/subscribe")
    ResponseEntity<ReadHistorySummaryResult> getSubscribeHistory(
            @Parameter(description = "회원 휴대폰번호", example = "01011112222", required = true)
            @RequestParam(name = "phone") @NotBlank String phone);

    @Operation(summary = "구독(최초 가입/등급 상승)",
            description = "채널 능력 → 전이 가능성 → csrng → 상태 전이 → 이력 기록 순으로 처리한다. "
                    + "형식 검증은 @Valid, 비즈니스 규칙은 엔티티·서비스가 담당.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구독 성공"),
            @ApiResponse(responseCode = "400", description = "형식 오류/잘못된 enum, 또는 구독 불가 채널",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "채널 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 상태 전이",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "csrng 비즈니스 실패(롤백)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "csrng 인프라 장애(재시도/서킷)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/subscribe")
    ResponseEntity<Void> postSubscribe(@Valid @RequestBody SubscribeRequest request);

    @Operation(summary = "해지(등급 하향/구독 종료)",
            description = "채널 능력 → 전이 가능성 → csrng → 상태 전이 → 이력 기록 순으로 처리한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "해지 성공"),
            @ApiResponse(responseCode = "400", description = "형식 오류/잘못된 enum, 또는 해지 불가 채널",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "채널/회원 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "허용되지 않는 상태 전이",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "csrng 비즈니스 실패(롤백)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "csrng 인프라 장애(재시도/서킷)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/unsubscribe")
    ResponseEntity<Void> postUnsubscribe(@Valid @RequestBody CancelRequest request);
}
