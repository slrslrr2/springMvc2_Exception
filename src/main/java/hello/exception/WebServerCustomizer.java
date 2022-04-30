package hello.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 해당 Controller는 ErrorPage를 직접 설정해주는 것이기에
 * SpringBoot에서 제공해주는 [BasicErrorController]를 사용하면
 *      아래와 같은 설정 없이 templates/error/html들만 만들어주면된다
 *
 * 그렇기에 //@Component 주석처리 해주었다.
 */
@Slf4j
@Component
public class WebServerCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error-page/404");
        ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error-page/500");
        ErrorPage errorPageEx = new ErrorPage(RuntimeException.class, "/error-page/500");

        factory.addErrorPages(errorPage404, errorPage500, errorPageEx);
    }
}
