package io.github.ingkoon.artinus.common.client.csrng.exception;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;

public class ExternalApiFailedException extends BusinessException {
    public ExternalApiFailedException() {
        super(ErrorCode.EXTERNAL_API_FAILED);
    }
}
