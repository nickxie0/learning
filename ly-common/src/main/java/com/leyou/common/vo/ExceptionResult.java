package com.leyou.common.vo;

import com.leyou.common.exceptions.LyException;
import lombok.Data;

import java.util.Date;

@Data
public class ExceptionResult {
    private int status;
    private String message;
    private String timeStamp;

    public ExceptionResult(LyException e) {
        this.status = e.getStatus();
        this.message = e.getMessage();
        this.timeStamp = new Date().toLocaleString();
    }
}
