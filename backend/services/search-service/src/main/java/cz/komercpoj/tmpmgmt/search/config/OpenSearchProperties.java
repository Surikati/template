package cz.komercpoj.tmpmgmt.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmpmgmt.opensearch")
public record OpenSearchProperties(
    String host, int port, String scheme, String username, String password) {

  public OpenSearchProperties {
    if (host == null) host = "localhost";
    if (port == 0) port = 9200;
    if (scheme == null) scheme = "http";
  }
}
