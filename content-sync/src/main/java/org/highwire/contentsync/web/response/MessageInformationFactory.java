package org.highwire.contentsync.web.response;

import org.highwire.authsdk.caller.CallerContext;
import org.highwire.authsdk.tracing.RequestCorrelationFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** One place that builds MessageInformation — same pattern as iam-service's factory. */
@Component
public class MessageInformationFactory {

    public MessageInformation create() {
        String trackingNumber = MDC.get(RequestCorrelationFilter.MDC_KEY);
        return new MessageInformation(trackingNumber, CallerContext.get());
    }
}
