package com.leyou.common.exceptions;

import com.leyou.common.enums.ExceptionEnum;
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

    public LyException(ExceptionEnum em) {
        super(em.getMessage());
        this.status = em.getStatus();
    }
    public LyException(ExceptionEnum em, Throwable cause) {
        super(em.getMessage(), cause);
        this.status = em.getStatus();
    }
}
