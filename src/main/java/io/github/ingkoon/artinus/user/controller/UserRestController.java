package io.github.ingkoon.artinus.user.controller;

import io.github.ingkoon.artinus.user.controller.request.CancelRequest;
import io.github.ingkoon.artinus.user.controller.request.SubscribeRequest;
import io.github.ingkoon.artinus.user.service.UserService;
import io.github.ingkoon.artinus.user.service.param.ReadHistoryParam;
import io.github.ingkoon.artinus.user.service.result.ReadHistorySummaryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 라우팅·검증·문서는 {@link UserApi}에 선언하고, 컨트롤러는 서비스 위임만 담당한다.
 */
@RequestMapping("/api/user")
@RestController
@RequiredArgsConstructor
@Validated
public class UserRestController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<ReadHistorySummaryResult> getSubscribeHistory(String phone) {
        return ResponseEntity.ok(userService.getHistory(new ReadHistoryParam(phone)));
    }

    @Override
    public ResponseEntity<Void> postSubscribe(SubscribeRequest request) {
        userService.subscribe(request.toServiceParam());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> postUnsubscribe(CancelRequest request) {
        userService.cancel(request.toServiceParam());
        return ResponseEntity.ok().build();
    }
}
