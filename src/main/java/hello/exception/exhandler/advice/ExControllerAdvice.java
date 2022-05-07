package hello.exception.exhandler.advice;

import hello.exception.exception.UserException;
import hello.exception.exhandler.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExControllerAdvice {
    /**
     해당 Controller안에 생긴 IllegalArgumentException를 잡는다.
     return ErrorResult를 Json Data로 날린다.
     안에 @RestController로 선언하였기에
     @ResponseBody도 있기에 자동으로 JSON 가능한것.
     정상적으로 Return한것으로 처리하기때문에
     @ResponseStatus(HttpStatus.BAD_REQUEST)를 안붙이면 200코드가 표시된다.
     **/
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult iIIegalExHandler(IllegalArgumentException e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("BAD", e.getMessage());
    }

    //    @ExceptionHandler(UserException.class) // Parameter로 UserException받으니까 생략 가능
    @ExceptionHandler
    public ResponseEntity<ErrorResult> userExHandler(UserException e){
        log.error("[exceptionHandler] ex", e);
        ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
        return new ResponseEntity(errorResult, HttpStatus.BAD_REQUEST);
    }

    /**
     위에서 처리하지 못한 에러를 모두 받아들인다.
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public ErrorResult exHandler(Exception e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("EX", "내부 오류");
    }
}
