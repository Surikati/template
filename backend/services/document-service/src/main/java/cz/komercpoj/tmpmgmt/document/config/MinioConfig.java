package cz.komercpoj.tmpmgmt.document.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

  private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

  @Bean
  MinioClient minioClient(MinioProperties props) {
    return MinioClient.builder()
        .endpoint(props.endpoint())
        .credentials(props.accessKey(), props.secretKey())
        .build();
  }

  /** Ensures the target bucket exists on startup. Idempotent. */
  @Bean
  ApplicationRunner minioBucketBootstrap(MinioClient client, MinioProperties props) {
    return args -> {
      boolean exists =
          client.bucketExists(BucketExistsArgs.builder().bucket(props.bucket()).build());
      if (!exists) {
        log.info("Creating MinIO bucket '{}'", props.bucket());
        client.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
      }
    };
  }
}
