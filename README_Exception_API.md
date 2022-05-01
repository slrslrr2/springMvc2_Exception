

```java
@Slf4j
@Component // 서블릿이 제공하는 에러페이지 만들기
public class WebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error-page/404");
        ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error-page/500");
        ErrorPage errorPageEx = new ErrorPage(RuntimeException.class, "/error-page/500");

        factory.addErrorPages(errorPage404, errorPage500, errorPageEx);
    }
}
```

<img width="1677" alt="image-20220430233921016" src="https://user-images.githubusercontent.com/58017318/166145694-e0d58862-5412-4ca7-be3a-726816f20b01.png">
> 1. @GetRequestMapping("/api/member/{id}") 로 Mapping을 선언하였기때문에<br>@PathValiable을 사용하여 id Argument를 받았다.
>
> 2. Client는 **Accept**를 **application/json**로 지정하였고<br>서버에서는 @RequestMapping에 **produces = MediaType.APPLICATION_JSON_VALUE**를 지정함으로써 같은 @RequestMapping으로 지정된 URL일지라도 **errorPage500Api** 메소드로 매핑되게된다.



-----

만약 WebServerCustomizer.java @Component를 주석처리한 뒤,<br>**http://localhost:8080/api/members/ex** 해당 URL로 날리면 어떻게 될까?

요청할 경우 Accept를 application/json으로 날렸기때문에<br>BasicErrorController가 자동으로 응답시에 JSON코드로 리턴해준다.

<img width="635" alt="image-20220430235826113" src="https://user-images.githubusercontent.com/58017318/166145697-61cdd3c8-31e6-41b8-8bbd-3f9cee0ca76e.png">


그럼 요청할 경우 Accept를 text/html로 날리면?<br>이전에 우리가 만들었던 500 Page를 표시해준다.

<img width="709" alt="image-20220501000210305" src="https://user-images.githubusercontent.com/58017318/166145699-0e211113-de22-498d-b78c-dea241454c10.png">
> BasicErrorController 는 HTML 페이지를 제공하는 경우에는 매우 편리하다.
>
> 그런데 API 오류 처리는 다른 차원의 이야기이다. API 마다, 각각의 컨트롤러나 예외마다 서로 다른 응답 결과를 출력해야 할 수도 있다. 그렇다면 마지막으로 배울 **@ExceptionHandler**를 사용하면 되는데<br>이를 이해하기 위해서는 **HandlerExceptionResolver**부터 알아야한다.



------

## HandlerExceptionResolver

**ApiExceptionController**

```java
@Slf4j
@RestController
public class ApiExceptionController {
    @GetMapping("/api/members/{id}")
    public MemberDto getMember(@PathVariable String id){
        if(id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }
				
        // 추가
        if(id.equals("bad")){
            throw new IllegalArgumentException("잘못된 입력 값");
        }

        return new MemberDto(id, "hello " + id);
    }
}
```

> id가 bad로 넘어올 경우 **IllegalArgumentException**로 return 한다.



<img width="535" alt="image-20220501004604202" src="https://user-images.githubusercontent.com/58017318/166145700-ba0c9cc5-d9bd-419c-97c4-4ee1503e8ef0.png">
> ExceptionResolver를 적용하기 전에는<br>1. Controller IllegalArgumentException이 발생하고 Servlet -> WAS까지 타고 올라가서 500에러 발생<br>.       - Error가 발생하였으니 postHandle은 실행X



<img width="539" alt="image-20220501004750946" src="https://user-images.githubusercontent.com/58017318/166145701-f577fbbe-4a65-40a5-80f8-9dd5cd37cf06.png">
> ExceptionRosolver 적용 후에는 <br>Error가 발생하였으니 postHandle은 실행X은 똑같지만<br>**HandlerExceptionResolver**을 상속받아 **오류 잡아먹는 코드**를 넣어줄 것이다.



```java
public interface HandlerExceptionResolver {
    ModelAndView resolveException(
      HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex);
}
```



#### HandlerExceptionResolver를 상속받은<br>IllegalArgumentException 오류잡아먹는 코드 만들기

```java
@Slf4j
public class MyHandlerExceptionResolver implements HandlerExceptionResolver {
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if (ex instanceof IllegalArgumentException) {
                log.info("IllegalArgumentException resolver to 400");
//                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return new ModelAndView();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
```



```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
    resolvers.add(new MyHandlerExceptionResolver());
  }
```



> > http://localhost:8080/api/members/bad 로 요청 시에 <br>REQUEST -> Filter -> Servlet -> Intercepter -> controller까지 와서 <br>Exception IllegalArgumentException이 터진다. 
> >
> > 그럼 Exception 500이 발생하여 위로 타고 올라가는데 이때 3번째 단계인 Servlet에서 **HandlerExceptionResolver**이 만들어져 있는것을 보고 
>
> 1. 만약 response.sendError(HttpServletResponse.SC_BAD_REQUESET);를 주석처리히면<br>아래와 같이 Exception을 잡어먹었고, return new ModelAndView를 하였기때문에<br>Servlet -> WAS까지 정상 처리되어 빈 화면을 리턴해준다.<br><img width="562" alt="image-20220501005325494" src="https://user-images.githubusercontent.com/58017318/166145703-8752b75f-16f9-4516-9a18-b01050414fb2.png">
>
> 2. 만약 response.sendError(HttpServletResponse.SC_BAD_REQUESET);를 주석을 풀면<br>Exception을 집어먹긴 하였지만 sendError를 400으로 지정해주었기 때문에<br>Servlet에서 WAS까지 타고 올라가서 BasicErrorController에 의해<br>Client가 보낸 Accept에 의해 Page로 보낼지, json으로 보낼지 판단하여 return 한다. <br>
>
>    <img width="484" alt="image-20220501005911523" src="https://user-images.githubusercontent.com/58017318/166145705-8ffe50c2-28d2-447e-997b-1867cde85516.png">



내용 정리

**반환 값에 따른 동작 방식**<br>HandlerExceptionResolver 의 반환 값에 따른 DispatcherServlet 의 동작 방식은 다음과 같다.

**빈 ModelAndView**<br>new ModelAndView() 처럼 빈 ModelAndView 를 반환하면 뷰를 렌더링 하지 않고, 정상 흐름으로 서블릿이 리턴된다.
	
**ModelAndView 지정**<br>ModelAndView 에 View , Model 등의 정보를 지정해서 반환하면 뷰를 렌더링 한다.
	
**null:**<br>null 을 반환하면, 다음 ExceptionResolver 를 찾아서 실행한다. 
	만약 처리할 수 있는 ExceptionResolver 가 없으면 예외 처리가 안되고, 
	기존에 발생한 예외를 서블릿 밖으로 던진다. 
	
**ExceptionResolver 활용 예외 상태 코드 변환**<br>예외를 response.sendError(xxx) 호출로 변경해서 서블릿에서 상태 코드에 따른 오류를 처리하도록 위임 이후<br>WAS는서블릿오류페이지를 찾아서 내부호출, 예를들어서스프링부트가기본으로설정한 / error 가 호출됨

**뷰 템플릿 처리**
ModelAndView 에 값을 채워서 예외에 따른 새로운 오류 화면 뷰 렌더링 해서 고객에게 제공

**API 응답 처리**
response.getWriter().println("hello"); 처럼 HTTP 응답 바디에 직접 데이터를 넣어주는
것도 가능하다. 여기에 JSON 으로 응답하면 API 응답 처리를 할 수 있다.

-----



HandlerExceptionResolver를 상속받은 MyHandlerExceptionResolver에서는 <br>**response.sendError(HttpServletResponse.SC_BAD_REQUEST);**를 통해 상태코드를 지정할 순있지만<br>**ExceptionResolver**에서 sendError로 Exception을 지정하여<br>Exception을 발생시켜 WAS까지 타고 올라가 <br>BasicErrorController를 타고 내려가야한다.<br>이건 너무 복잡하므로<br> UserHandlerExceptionResolver를 하나 만들어서 <br>WAS에서 타고 다시 리턴 되는 방식이 아닌, HandlerExceptionResolver에서 끝내고 WAS에서는 정상처리인것처럼 만들어보자

```java
@Slf4j
@RestController
public class ApiExceptionController {
    @GetMapping("/api/members/{id}")
    public MemberDto getMember(@PathVariable String id){
        if(id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }

        if(id.equals("bad")){
            throw new IllegalArgumentException("잘못된 입력 값");
        }
				
      	// 추가
        if(id.equals("user-ex")){
            throw new UserException("사용자 오류");
        }

        return new MemberDto(id, "hello " + id);
    }

```

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add(new MyHandlerExceptionResolver());
      
      	// 추가
        resolvers.add(new UserHandlerExceptionResolver());
    }
```



```java
@Slf4j
public class UserHandlerExceptionResolver implements HandlerExceptionResolver {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex instanceof UserException) {
            log.info("UserException resolver to 400");
            String accept = request.getHeader("accept");
          
            // response.sendError가 아닌
            // response.setStatus를 사용하여 상태코드 저장
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try {
                if ("application/json".equals(accept)) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("ex", ex.getClass());
                    errorResult.put("message", ex.getMessage());
                    String result = objectMapper.writeValueAsString(errorResult);

                    response.setContentType("application/json");
                    response.setCharacterEncoding("utf-8");
                    response.getWriter().write(result);

                    return new ModelAndView();
                } else {
                    return new ModelAndView("error/500");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return  null;
    }
}

```

---

**HandlerExceptionResolver**을 하나하나 모두 만들기에는 우리가 너무 힘들다.

그래서 Spring에서 제공해주는 HandlerExceptionResolver를 사용해보자

1. **ExceptionHandlerExceptionResolver**
   - @ExceptionHandler 을 처리한다. API 예외 처리는 대부분 이 기능으로 해결한다. 조금 뒤에 자세히 설명한다.
2. **ResponseStatusExceptionResolver**
   - HTTP 상태 코드를 지정해준다.<br>예) @ResponseStatus(value = HttpStatus.NOT_FOUND)
   - ResponseStatusException을 던지기
3. **DefaultHandlerExceptionResolver**
   - 스프링 내부 기본 예외를 처리한다.



------

# 2. ResponseStatusExceptionResolver

우리는 우선 **2. ResponseStatusExceptionResolver**를 활용해볼것이다.





**ApiExceptionController.java**

```java
@GetMapping("/api/response-status-ex1")
public String reponseStatusEx1(){
  throw new BadRequestException();
}
```



@ResponseStatus를 확용하는 방법이 있고<br>**BadRequestException.java**

```java
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "잘못된 요청 오류")
public class BadRequestException extends RuntimeException{
}
```



ResponseStatusException를 retrun 하는 방법이 있다.

```java
@GetMapping("/api/response-status-ex2")
public String reponseStatusEx2(){
  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.bad", new IllegalArgumentException());
}
```



@ResponseStatus을 실행하면 HandlerExceptionResolver를 상속받은 **<br>2. ResponseStatusExceptionResolver**가 **BadRequestException**을 잡아먹고 WAS에서는 정상처리 해준다

<img width="1666" alt="image-20220501104704919" src="https://user-images.githubusercontent.com/58017318/166145706-1bbabe03-5a8e-4f40-a729-ddcd82f1e744.png">

**ResponseStatusExceptionResolver**를 까보면 HandlerExceptionResolver를 상속받았기에<br>resolverException메소드를 구현해야하고<br>그 안에는 **doResolveException** **ModelAndView로 return 하였다**

그럼 좀더 소스를 들여다보면

```java
if (status != null) {
  return resolveResponseStatus(status, request, response, handler, ex);
}
```

```java
protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
                                             HttpServletResponse response, @Nullable Object handler, Exception ex) throws Exception {
	
  // 상태코드와 reason선언된 값을 가져와서
  int statusCode = responseStatus.code().value();
  String reason = responseStatus.reason();
  
  // applyStatusAndReason에 보낸다.
  return applyStatusAndReason(statusCode, reason, response);
}
```

```java
protected ModelAndView applyStatusAndReason(int statusCode, @Nullable String reason, HttpServletResponse response)
  throws IOException {

  if (!StringUtils.hasLength(reason)) {
    response.sendError(statusCode);
  } else {
    
    // messageSource에서도 활용할 수 있다!!
    String resolvedReason = (this.messageSource != null ?
                             this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
                             reason);
    // sendError를 만들어 보낸다.
    response.sendError(statusCode, resolvedReason);
  }
  return new ModelAndView();
}
```

> reason을 보낼 때 messageSource를 확인해보기 때문에<br>messages.properties의 파일에 reason을 명시하면 사용할 수 있다.
>
> <img width="652" alt="image-20220501110549049" src="https://user-images.githubusercontent.com/58017318/166145707-68fe7169-4152-4087-a706-5d7501e04e59.png">


-----

# 3. DefalutHandlerExceptionResolver

```java
@GetMapping("/api/default-handler-ex2")
public String defaultException(@RequestParam Integer data){
    return "ok";
}
```

Client가 http://localhost:8080/api/default-handler-ex2?data=qqq로 전송할 경우

**MethodArgumentTypeMismatchException**이 발생하는데, 이는 원래대로라면 500을 뱉어야하지만<br>아래와 같이 400을 뱉어준다. 어떻게 된 것일까?

<img width="829" alt="image-20220501112744565" src="https://user-images.githubusercontent.com/58017318/166145709-03068993-60b6-4d17-bd20-e00e78da18cd.png">


DefaultHandlerExceptionResolver.java에 들어가면

```java
@Override
@Nullable
protected ModelAndView doResolveException(
  HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {
  	
  /// 생략 ////
  else if (ex instanceof TypeMismatchException) {
    return handleTypeMismatch(
      (TypeMismatchException) ex, request, response, handler);
  }

```

```java
protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
                                          HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

  response.sendError(HttpServletResponse.SC_BAD_REQUEST); // 400으로 던져준다.
  return new ModelAndView();
}
```



```java
지금까지 다음 ExceptionResolver 들에 대해 알아보았다.
  1. ExceptionHandlerExceptionResolver -> 다음 시간에
  2. ResponseStatusExceptionResolver -> HTTP 응답 코드 변경
  3. DefaultHandlerExceptionResolver -> 스프링 내부 예외 처리
```



지금까지 HTTP 상태 코드를 변경하고, 스프링 내부 예외의 상태코드를 변경하는 기능도 알아보았다. <br>그런데 HandlerExceptionResolver 를 직접 사용하기는 복잡하다. <br>API 오류 응답의 경우 **response.sendError** 에 직접 데이터를 넣어야 해서 매우 **불편하고 번거롭다.** **ModelAndView 를 반환**해야 하는 것도 **API에는 잘 맞지 않는다.**

------



# 1. ExceptionHandlerExceptionResolver

@ExceptionHandler 라는 어노테이션을 사용하면 예외처리를 쉽게 처리할 수 있다.



```java
@Slf4j
@RestController
public class ApiExceptionV2Controller {
    /**
        해당 Controller안에 생긴 IllegalArgumentException를 잡는다.
        return ErrorResult를 Json Data로 날린다.
            안에 @RestController로 선언하였기에
            @ResponseBody도 있기에 자동으로 JSON 가능한것.
            
        정상적으로 Return한것으로 처리하기때문에 WAS에서 에러를 안던지고
        @ResponseStatus(HttpStatus.BAD_REQUEST)를 안붙이면 200코드가 표시된다.
    **/
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult iIIegalExHandler(IllegalArgumentException e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("BAD", e.getMessage());
    }

//    @ExceptionHandler(UserException.class) 
  // Parameter로 UserException받으니까 생략 가능
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

    @GetMapping("/api2/members/{id}")
    public MemberDto getMember(@PathVariable String id){
        if(id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }

        if(id.equals("bad")){
            throw new IllegalArgumentException("잘못된 입력 값");
        }

        if(id.equals("user-ex")){
            throw new UserException("사용자 오류");
        }

        return new MemberDto(id, "hello " + id);
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String memberId;
        private String name;
    }
}

```



자, 그럼 소스 코드를 하나씩 들여다보자

#### iIIegalExHandler

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
@ExceptionHandler(IllegalArgumentException.class)
public ErrorResult iIIegalExHandler(IllegalArgumentException e){
  log.error("[exceptionHandler] ex", e);
  return new ErrorResult("BAD", e.getMessage());
}
```

/api2/members/**bad**<br>컨트롤러를 호출한 결과 IllegalArgumentException 예외가 컨트롤러 밖으로 던져진다. <br>예외가 발생했으로 ExceptionResolver 가 작동한다. <br>가장 우선순위가 높은 ExceptionHandlerExceptionResolver 가 실행된다.

ExceptionHandlerExceptionResolver는 해당 컨트롤러에 IllegalArgumentException 을 처리할 수 있는 @ExceptionHandler 가 있는지 확인한다.

illegalExHandle() 를 실행한다. <br>@RestController 이므로 illegalExHandle() 에도 @ResponseBody 가 적용된다. <br>따라서 HTTP 컨버터가 사용되고, 응답이 다음과 같은 JSON으로 반환된다.

@ResponseStatus(HttpStatus.BAD_REQUEST) 를 지정했으므로 HTTP 상태 코드 400으로 응답한다.<br>만약 지정하지 않았다면 200을 던진다.



```
// 응답
{
  "code": "BAD",
  "message": "잘못된 입력 값" 
}
```



#### **UserException**

```java
@ExceptionHandler
public ResponseEntity<ErrorResult> userExHandle(UserException e) {
  log.error("[exceptionHandle] ex", e);
  ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
  return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
}
```

@ExceptionHandler 에 예외를 지정하지 않으면 해당 메서드 파라미터 예외를 사용한다. <br>여기서는 UserException 을 사용한다.

ResponseEntity 를 사용해서 HTTP 메시지 바디에 직접 응답한다. 물론 HTTP 컨버터가 사용된다.

ResponseEntity 를 사용하면 HTTP 응답 코드를 프로그래밍해서 동적으로 변경할 수 있다. <br>앞서 살펴본 @ResponseStatus 는 애노테이션이므로 HTTP 응답 코드를 동적으로 변경할 수 없다.



#### **Exception**

```java
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
@ExceptionHandler
public ErrorResult exHandle(Exception e) {
  log.error("[exceptionHandle] ex", e);
  return new ErrorResult("EX", "내부 오류");
}
```

thrownewRuntimeException("잘못된 사용자")이코드가실행되면서,<br>컨트롤러밖으로 RuntimeException 이 던져진다.

RuntimeException 은 Exception 의 자식 클래스이다. 따라서 이 메서드가 호출된다. <Br>@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) 로 HTTP 상태 코드를 500으로 응답한다.

-------

# ControllerAdvice

ExControllerAdvice를 만들어서 <br>ApiExceptionV2Controller안에 선언된 @ExceptionHandler 내용을 복사해 넣자



```java
@Slf4j
@RestControllerAdvice
public class ExControllerAdvice {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResult iIIegalExHandler(IllegalArgumentException e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("BAD", e.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResult> userExHandler(UserException e){
        log.error("[exceptionHandler] ex", e);
        ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
        return new ResponseEntity(errorResult, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public ErrorResult exHandler(Exception e){
        log.error("[exceptionHandler] ex", e);
        return new ErrorResult("EX", "내부 오류");
    }
}
```

위에처럼 선언하면 모든 Controller의 Exception을 받아들어주는데

**@ControllerAdvice**<br>@ControllerAdvice 는 대상으로 지정한 여러 컨트롤러에 <br>@ExceptionHandler , @InitBinder 기능을 부여해주는 역할을 한다.

@ControllerAdvice 에 대상을 지정하지 않으면 모든 컨트롤러에 적용된다. (글로벌 적용)

@RestControllerAdvice 는 @ControllerAdvice 와 같고, <br>@ResponseBody 가 추가되어 있다. @Controller , @RestController 의 차이와 같다.



**대상 Controller를 지정할 수도 있다.**

```java
// Target all Controllers annotated with @RestController
@ControllerAdvice(annotations = RestController.class)
public class ExampleAdvice1 {}

// Target all Controllers within specific packages
@ControllerAdvice("org.example.controllers")
public class ExampleAdvice2 {}

// Target all Controllers assignable to specific classes
@ControllerAdvice(assignableTypes = {ControllerInterface.class,
                                     AbstractController.class})
public class ExampleAdvice3 {}
```



### 정리

### @ExceptionHandler 와 @ControllerAdvice 를 조합하면 예외를 깔끔하게 해결할 수 있다.
