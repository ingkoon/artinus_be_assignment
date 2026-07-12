package io.github.ingkoon.artinus.user.controller;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;
import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import io.github.ingkoon.artinus.user.service.result.HistoryItem;
import io.github.ingkoon.artinus.user.service.result.ReadHistorySummaryResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static io.github.ingkoon.artinus.user.enums.UserStatus.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRestController.class)
class UserRestControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;

    @Test
    void 구독_성공_200() throws Exception {
        mockMvc.perform(post("/api/user/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01011112222\",\"channelId\":1,\"target\":\"BASIC\"}"))
                .andExpect(status().isOk());

        verify(userService).subscribe(any());
    }

    @Test
    void 필수값_누락_400() throws Exception {
        mockMvc.perform(post("/api/user/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01011112222\",\"channelId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C002"));

        verifyNoInteractions(userService);
    }

    @Test
    void 잘못된_enum_400() throws Exception {
        mockMvc.perform(post("/api/user/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01011112222\",\"channelId\":1,\"target\":\"GOLD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C002"));
    }

    @Test
    void 비즈니스예외_상태코드_매핑() throws Exception {
        doThrow(new BusinessException(ErrorCode.CHANNEL_NOT_SUBSCRIBABLE))
                .when(userService).subscribe(any());

        mockMvc.perform(post("/api/user/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01011112222\",\"channelId\":5,\"target\":\"BASIC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CH002"));
    }

    @Test
    void 낙관적락_충돌_409() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(User.class, 1L))
                .when(userService).subscribe(any());

        mockMvc.perform(post("/api/user/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01011112222\",\"channelId\":1,\"target\":\"PREMIUM\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("U002"));
    }

    @Test
    void 무결성위반_최초가입_충돌_409() throws Exception {
        doThrow(new DataIntegrityViolationException("duplicate phone"))
                .when(userService).subscribe(any());

        mockMvc.perform(post("/api/user/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01011112222\",\"channelId\":1,\"target\":\"BASIC\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("U002"));
    }

    @Test
    void 해지_성공_200() throws Exception {
        mockMvc.perform(post("/api/user/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01033334444\",\"channelId\":1,\"target\":\"NONE\"}"))
                .andExpect(status().isOk());

        verify(userService).cancel(any());
    }

    @Test
    void 이력조회_성공_200() throws Exception {
        ReadHistorySummaryResult result = new ReadHistorySummaryResult(
                List.of(new HistoryItem("홈페이지", NONE, BASIC, LocalDateTime.of(2026, 1, 1, 10, 0))),
                "요약 문장");
        when(userService.getHistory(any())).thenReturn(result);

        mockMvc.perform(get("/api/user/subscribe").param("phone", "01011112222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("요약 문장"))
                .andExpect(jsonPath("$.history[0].channelName").value("홈페이지"));
    }

    @Test
    void 이력조회_phone_누락_400() throws Exception {
        mockMvc.perform(get("/api/user/subscribe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C002"));
    }
}
