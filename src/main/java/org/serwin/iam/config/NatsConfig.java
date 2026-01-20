package org.serwin.iam.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class NatsConfig {

    @Value("${nats.url}")
    private String natsUrl;

    @Value("${nats.username}")
    private String natsUsername;

    @Value("${nats.password}")
    private String natsPassword;

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        io.nats.client.Options options = new io.nats.client.Options.Builder()
                .server(natsUrl)
                .userInfo(natsUsername, natsPassword)
                .build();
        return Nats.connect(options);
    }
}
