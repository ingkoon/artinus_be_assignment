package io.github.ingkoon.artinus.user.service;

import io.github.ingkoon.artinus.user.service.param.CancelParam;
import io.github.ingkoon.artinus.user.service.param.ReadHistoryParam;
import io.github.ingkoon.artinus.user.service.param.SubscribeParam;
import io.github.ingkoon.artinus.user.service.result.ReadHistorySummaryResult;

public interface UserService {
    void subscribe(SubscribeParam param);
    void cancel(CancelParam param);
    ReadHistorySummaryResult getHistory(ReadHistoryParam param);
}
