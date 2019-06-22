package com.leyou.common.exceptions;

import lombok.Data;
import lombok.Getter;

@Getter
public class LyException extends RuntimeException {
    private Integer status;

    public LyException(Integer status, String message) {
        super(message);
        this.status = status;
    }

    public LyException(Integer status,String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
