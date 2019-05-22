package com.leyou.common.advice;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.ExceptionResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * ClassName:CommonExceptionHandler
 * PackageName:com.leyou.common.advice
 * Description:
 *
 * @Date:2019/3/14 20:57
 * @Author:yuqin_su@163.com
 */

@ControllerAdvice
//通过@ControllerAdvice注解可以将对于控制器的全局配置放在同一个位置。
//　　注解了@Controller的类的方法可以使用@ExceptionHandler、@InitBinder、@ModelAttribute注解到方法上。
//　　@ControllerAdvice注解将作用在所有注解了@RequestMapping的控制器的方法上
public class CommonExceptionHandler {

    @ExceptionHandler(LyException.class)
    public ResponseEntity<ExceptionResult> handleException(LyException e){
        return ResponseEntity.status(e.getExceptionEnum().getCode())
                .body(new ExceptionResult(e.getExceptionEnum()));
    }
}
