package org.highwire.iam.web.response;

import org.highwire.authsdk.caller.CallerInfo;

/**
 * trackingNumber is deliberately the SAME value as auth-sdk's
 * RequestCorrelationFilter puts in MDC and the X-Request-Id response
 * header — one id end-to-end, so a caller can correlate this field, the
 * response header, and server-side logs for the same request without
 * reconciling two different ids.
 */
public record MessageInformation(String trackingNumber, CallerInfo userInfo) {
}
