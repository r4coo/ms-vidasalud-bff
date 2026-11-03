package cl.vidasalud.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class Downstreams {
    @Bean("appointments")
    Downstream appointments(RestClient.Builder b, @Value("${endpoints.appointments}") String url) {
        return new Downstream(b.clone(), url);
    }

    @Bean("catalog")
    Downstream catalog(RestClient.Builder b, @Value("${endpoints.catalog}") String url) {
        return new Downstream(b.clone(), url);
    }

    @Bean("rabbitAdmin")
    Downstream rabbitAdmin(RestClient.Builder b, @Value("${endpoints.rabbit-admin}") String url) {
        return new Downstream(b.clone(), url);
    }
}
