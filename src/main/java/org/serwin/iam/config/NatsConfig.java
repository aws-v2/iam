package org.serwin.iam.config;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.StreamConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
@Slf4j
public class NatsConfig {

    @Value("${nats.url}")
    private String natsUrl;

    @Value("${nats.username}")
    private String natsUsername;

    @Value("${nats.password}")
    private String natsPassword;

    @Value("${spring.profiles.active:dev}")
    private String env;

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        io.nats.client.Options options = new io.nats.client.Options.Builder()
                .server(natsUrl)
                .userInfo(natsUsername, natsPassword)
                .build();
        return Nats.connect(options);
    }

    @Bean
    public JetStream jetStream(Connection connection, JetStreamManagement jsm) throws IOException {
        return connection.jetStream();
    }

    @Bean
    public JetStreamManagement jetStreamManagement(Connection connection)
            throws IOException, io.nats.client.JetStreamApiException {
        JetStreamManagement jsm = connection.jetStreamManagement();

        // Stream listens broadly so JetStream captures all inbound policy events.
        // The consumer-level filter + service guard in PolicyRegistrationConsumer
        // ensures IAM never processes its own outbound responses.
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name("IAM_POLICIES")
                .subjects(env + ".*.v1.policy.*")
                .maxAge(Duration.ofDays(7))
                .build();

        try {
            jsm.getStreamInfo("IAM_POLICIES");
            jsm.updateStream(streamConfig);
        } catch (io.nats.client.JetStreamApiException e) {
            jsm.addStream(streamConfig);
        }

        // Delete stale durable consumers so they are recreated with the correct
        // subject filter on next subscribe. Safe to call on every startup.
        for (String staleDurable : new String[] { "iam-policy-registrar-v2", "iam-policy-registrar-v3" }) {
            try {
                jsm.deleteConsumer("IAM_POLICIES", staleDurable);
                log.info("Deleted stale durable consumer: {}", staleDurable);
            } catch (io.nats.client.JetStreamApiException ignored) {
                // Consumer did not exist — that's fine.
            }
        }

        return jsm;
    }
}
