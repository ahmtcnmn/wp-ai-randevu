package com.appointflow.config;

import com.iyzipay.Options;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class IyzicoConfig {

    @Value("${app.iyzico.api-key:}")
    private String apiKey;

    @Value("${app.iyzico.secret-key:}")
    private String secretKey;

    @Value("${app.iyzico.base-url:https://api.iyzipay.com}")
    private String baseUrl;

    @Value("${app.iyzico.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.iyzico.dev-mode:false}")
    private boolean devMode;

    @Value("${app.iyzico.strict-buyer-info:false}")
    private boolean strictBuyerInfo;

    @Value("${app.iyzico.callback-url:http://localhost:8080/api/v1/billing/iyzico-callback}")
    private String callbackUrl;

    @Value("${app.iyzico.frontend-callback-url:http://localhost:3000/billing/callback}")
    private String frontendCallbackUrl;

    // Sandbox defaults (kullanılır env var boş olduğunda)
    private static final String SANDBOX_API_KEY    = "sandbox-3qBYqNy2GTaDsdovxccV1AkSgMjmSGer";
    private static final String SANDBOX_SECRET_KEY = "sandbox-Ad9Ms7yis9Ik1Dt1icOqY7Pras91qIZ7";
    private static final String SANDBOX_BASE_URL   = "https://sandbox-api.iyzipay.com";

    @Bean
    public Options iyzicoOptions() {
        Options options = new Options();
        options.setApiKey(apiKey != null && !apiKey.isBlank() ? apiKey : SANDBOX_API_KEY);
        options.setSecretKey(secretKey != null && !secretKey.isBlank() ? secretKey : SANDBOX_SECRET_KEY);
        String url = baseUrl != null && !baseUrl.isBlank() ? baseUrl : SANDBOX_BASE_URL;
        // sandbox key varsa sandbox URL'i zorla
        String resolvedKey = options.getApiKey();
        if (resolvedKey != null && resolvedKey.startsWith("sandbox-") && !url.contains("sandbox")) {
            url = SANDBOX_BASE_URL;
        }
        options.setBaseUrl(url);
        return options;
    }
}
