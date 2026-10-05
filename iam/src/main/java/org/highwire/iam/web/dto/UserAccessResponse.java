package org.highwire.iam.web.dto;

import java.util.List;

/**
 * directRoles: assigned straight to the user, not via a group.
 * effectiveRoles: direct roles PLUS every role inherited from groups —
 * what the user can actually do, which is usually the one a UI wants to
 * show; groups/directRoles are what an admin edits to get there.
 */
public record UserAccessResponse(
        String username, List<String> groups, List<String> directRoles, List<String> effectiveRoles) {
}
