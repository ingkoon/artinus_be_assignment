package io.github.ingkoon.artinus.common.client.csrng.exception;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;

public class ExternalApiUnavailableException extends BusinessException {
    public ExternalApiUnavailableException() {
        super(ErrorCode.EXTERNAL_API_UNAVAILABLE);
    }
    public ExternalApiUnavailableException(Throwable cause) {
        super(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        initCause(cause);
    }
}
