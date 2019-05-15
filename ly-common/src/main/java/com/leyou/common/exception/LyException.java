package com.leyou.common.exception;

import com.leyou.common.enums.ExceptionEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ClassName:LyException
 * PackageName:com.leyou.common.exception
 * Description:
 *
 * @Date:2019/3/16 20:50
 * @Author:dianemax@163.com
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LyException extends RuntimeException{

    private ExceptionEnum exceptionEnum;

}
