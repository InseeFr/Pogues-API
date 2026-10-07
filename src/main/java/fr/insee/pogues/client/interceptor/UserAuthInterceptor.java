package fr.insee.pogues.client.interceptor;

import fr.insee.pogues.configuration.auth.user.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/** This interceptor will add the user token to http requests. */
@Slf4j
@RequiredArgsConstructor
public class UserAuthInterceptor implements ClientHttpRequestInterceptor {

    private final AuthenticationHelper authenticationHelper;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(authenticationHelper.getUserToken());
        return execution.execute(request, body);
    }
}
