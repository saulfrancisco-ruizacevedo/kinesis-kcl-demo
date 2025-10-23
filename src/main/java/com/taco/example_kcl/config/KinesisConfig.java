package com.taco.example_kcl.config;

import com.taco.example_kcl.processor.RecordProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.coordinator.Scheduler;

import java.net.URI;
import java.util.UUID;

@Slf4j
@Configuration
public class KinesisConfig {

    @Value("${aws.kinesis.stream-name}")
    private String streamName;

    @Value("${aws.kinesis.app-name}")
    private String appName;

    @Value("${aws.region}")
    private String region;

    @Autowired
    private Environment env;

    private static final String LOCALSTACK_DOCKER_ENDPOINT = "http://localstack:4566";

    private boolean isLocalEnvironment() {
        String environment = env.getProperty("ENV");
        return environment == null || "local".equalsIgnoreCase(environment);
    }

    private URI getEndpointOverride() {
        if (isLocalEnvironment()) {
            log.warn("Applying endpoint override: {}", LOCALSTACK_DOCKER_ENDPOINT);
            return URI.create(LOCALSTACK_DOCKER_ENDPOINT);
        }

        return null;
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (isLocalEnvironment()) {
            // This provider looks for AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
            // which are set in the docker-compose.yml for the kcl-consumer service.
            log.info("Using EnvironmentVariableCredentialsProvider for LocalStack.");
            return EnvironmentVariableCredentialsProvider.create();
        } else {
            // IAM Roles for Service Accounts
            log.info("Using DefaultCredentialsProvider.");
            return DefaultCredentialsProvider.create();
        }
    }

    @Bean
    public KinesisAsyncClient kinesisAsyncClient() {
        return KinesisAsyncClient.builder()
                .credentialsProvider(awsCredentialsProvider())
                .region(Region.of(region))
                .endpointOverride(getEndpointOverride())
                .build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        return DynamoDbAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(awsCredentialsProvider())
                .endpointOverride(getEndpointOverride())
                .build();
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(awsCredentialsProvider())
                .endpointOverride(getEndpointOverride())
                .build();
    }

    // Configure and run the KCL Scheduler
    @Bean
    public Scheduler kclScheduler(
            KinesisAsyncClient kinesisClient,
            DynamoDbAsyncClient dynamoDbClient,
            CloudWatchAsyncClient cloudWatchClient,
            RecordProcessorFactory recordProcessorFactory
    ) {
        // Each Pod must have a unique worker ID
        String workerId = appName + "-worker-" + UUID.randomUUID();
        log.info("Starting KCL Scheduler with workerId: {}", workerId);

        ConfigsBuilder configsBuilder = new ConfigsBuilder(
                streamName,
                appName,
                kinesisClient,
                dynamoDbClient,
                cloudWatchClient,
                workerId,
                recordProcessorFactory
        );

        Scheduler scheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig(),
                configsBuilder.leaseManagementConfig(),
                configsBuilder.lifecycleConfig(),
                configsBuilder.metricsConfig(),
                configsBuilder.processorConfig(),
                configsBuilder.retrievalConfig()
        );

        // Start the Scheduler in a separate, non-blocking thread
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setDaemon(true);
        schedulerThread.start();

        log.info("KCL Scheduler started.");
        return scheduler;
    }
}