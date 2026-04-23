package cz.komercpoj.tmpmgmt.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmpmgmt.minio")
public record MinioProperties(
        String endpoint, String accessKey, String secretKey, String bucket) {

    public MinioProperties {
        if (endpoint == null) endpoint = "http://localhost:9000";
        if (bucket == null) bucket = "tmpmgmt-documents";
    }
}
