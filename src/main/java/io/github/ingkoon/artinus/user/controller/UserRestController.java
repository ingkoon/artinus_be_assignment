package io.github.ingkoon.artinus.user.controller;

import io.github.ingkoon.artinus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping(value = "/api/user")
@RestController
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;

    @GetMapping("/subscribe")
    public ResponseEntity getSubscribeHistory() {
        return null;
    }

    @PostMapping("/subscribe")
    public ResponseEntity postSubscribe() {
        return null;
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity postUnsubscribe() {
        return null;
    }
}
