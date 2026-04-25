package cz.komercpoj.tmpmgmt.assembly.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards locale-shaping headers from the inbound HTTP request to outbound Feign calls.
 * Lets multi-tenant callers pick locale/timezone/currency once at the API gateway, and have
 * those choices applied by rendering-service when formatting variables.
 *
 * <p>Registered as a Spring bean → Feign auto-discovers and applies it to every client.
 * If there is no current request (background work, async retry), the interceptor silently
 * adds nothing and the downstream falls back to its server-wide defaults.
 */
@Component
public class LocaleHeaderForwardingInterceptor implements RequestInterceptor {

    private static final String[] FORWARDED_HEADERS = {"X-Locale", "X-Timezone", "X-Currency"};

    @Override
    public void apply(RequestTemplate template) {
        HttpServletRequest req = currentRequest();
        if (req == null) return;
        for (String name : FORWARDED_HEADERS) {
            String value = req.getHeader(name);
            if (value != null && !value.isBlank()) {
                template.header(name, value);
            }
        }
    }

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return (attrs instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
    }
}
