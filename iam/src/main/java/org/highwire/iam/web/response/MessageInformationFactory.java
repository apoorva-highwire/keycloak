package org.highwire.iam.web.response;

import org.highwire.authsdk.caller.CallerContext;
import org.highwire.authsdk.tracing.RequestCorrelationFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * One place that builds MessageInformation, so every controller does it
 * the same way — trackingNumber from the same MDC key
 * RequestCorrelationFilter populates, userInfo from CallerContext (set by
 * CallerResolvingFilter from the validated token — unresolved() if auth
 * hasn't run for this request, e.g. a request that failed authentication
 * before reaching a controller).
 */
@Component
public class MessageInformationFactory {

    public MessageInformation create() {
        String trackingNumber = MDC.get(RequestCorrelationFilter.MDC_KEY);
        return new MessageInformation(trackingNumber, CallerContext.get());
    }
}
