package hello.exception.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 ApiExceptionController에서
 throw new BadRequestException()을 날리면
 @ResponseStatus 으로 선언되어있기때문에
    ResponseStatusExceptionResolver를 선택하게된다.

 - ExceptionHandlerExceptionResolver
 - ResponseStatusExceptionResolver   (선택)
    - @
 - DefaltHandlerExceptionResolver
 */
//@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "잘못된 요청 오류")
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.bad")
public class BadRequestException extends RuntimeException{
}
