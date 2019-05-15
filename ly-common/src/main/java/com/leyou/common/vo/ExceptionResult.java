package com.leyou.common.vo;

import com.leyou.common.enums.ExceptionEnum;
import lombok.Data;

/**
 * ClassName:ExceptionResult
 * PackageName:com.leyou.common.vo
 * Description:
 *
 * @Date:2019/3/16 21:10
 * @Author:dianemax@163.com
 */
@Data
public class ExceptionResult {

    private int status;
    private String message;
    private Long timestamp;

    public ExceptionResult(ExceptionEnum em) {
        this.status = em.getCode();
        this.message = em.getMsg();
        this.timestamp = System.currentTimeMillis();
    }
}
