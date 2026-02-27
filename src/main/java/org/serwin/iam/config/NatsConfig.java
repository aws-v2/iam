package org.serwin.iam.config;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.StreamConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

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

    @Bean
    public JetStream jetStream(Connection connection) throws IOException {
        return connection.jetStream();
    }

    @Bean
    public JetStreamManagement jetStreamManagement(Connection connection)
            throws IOException, io.nats.client.JetStreamApiException {
        JetStreamManagement jsm = connection.jetStreamManagement();

        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name("IAM_POLICIES")
                .subjects(
                        "*.lambda.v1.policy.create", "*.lambda.v1.policy.update", "*.lambda.v1.policy.delete",
                        "*.lambda.v1.policy.get",
                        "*.ec2.v1.policy.create", "*.ec2.v1.policy.update", "*.ec2.v1.policy.delete",
                        "*.ec2.v1.policy.get",
                        "*.s3.v1.policy.create", "*.s3.v1.policy.update", "*.s3.v1.policy.delete", "*.s3.v1.policy.get")
                .maxAge(Duration.ofDays(7))
                .build();

        try {
            jsm.getStreamInfo("IAM_POLICIES");
            jsm.updateStream(streamConfig);
        } catch (io.nats.client.JetStreamApiException e) {
            jsm.addStream(streamConfig);
        }
        return jsm;
    }
}
