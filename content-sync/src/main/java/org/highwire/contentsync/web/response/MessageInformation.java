package org.highwire.contentsync.web.response;

import org.highwire.authsdk.caller.CallerInfo;

/**
 * trackingNumber is the same value auth-sdk's RequestCorrelationFilter puts
 * in MDC and the X-Request-Id response header — one id end-to-end, same
 * convention as iam-service.
 */
public record MessageInformation(String trackingNumber, CallerInfo userInfo) {
}
