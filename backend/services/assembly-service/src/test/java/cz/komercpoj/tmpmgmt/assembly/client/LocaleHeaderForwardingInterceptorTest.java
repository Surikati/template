package cz.komercpoj.tmpmgmt.assembly.client;

import static org.assertj.core.api.Assertions.*;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class LocaleHeaderForwardingInterceptorTest {

  private final LocaleHeaderForwardingInterceptor interceptor =
      new LocaleHeaderForwardingInterceptor();

  @AfterEach
  void clearContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static void bind(MockHttpServletRequest req) {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
  }

  @Test
  void forwardsAllThreeLocaleHeaders() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Locale", "en-GB");
    req.addHeader("X-Timezone", "Europe/London");
    req.addHeader("X-Currency", "GBP");
    bind(req);

    var template = new RequestTemplate();
    interceptor.apply(template);

    assertThat(template.headers().get("X-Locale")).containsExactly("en-GB");
    assertThat(template.headers().get("X-Timezone")).containsExactly("Europe/London");
    assertThat(template.headers().get("X-Currency")).containsExactly("GBP");
  }

  @Test
  void omitsHeadersThatAreNotPresent() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Locale", "de-DE"); // only locale set
    bind(req);

    var template = new RequestTemplate();
    interceptor.apply(template);

    assertThat(template.headers()).containsOnlyKeys("X-Locale");
  }

  @Test
  void omitsBlankHeaders() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Locale", "   "); // blank → ignored
    bind(req);

    var template = new RequestTemplate();
    interceptor.apply(template);

    assertThat(template.headers()).isEmpty();
  }

  @Test
  void noRequestContext_addsNothing() {
    // Background thread / async work — no inbound request bound to thread.
    var template = new RequestTemplate();
    interceptor.apply(template);

    assertThat(template.headers()).isEmpty();
  }
}
