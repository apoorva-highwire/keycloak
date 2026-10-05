package org.highwire.keycloak.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

/**
 * Injects a journal_codes claim onto the token — the exact claim shape
 * MULTI-TENANT-KEYCLOAK-IAM-PLAN.md's example JWT shows
 * ({@code journal_codes=[...]}), so every downstream service's
 * authorization middleware can read entitlement directly off the token
 * instead of calling back to the IAM service per request.
 *
 * Two sources, checked in order, matching the two shapes of token this
 * claim can ride on:
 *
 *   1. USER token (a reader logged in) — journal_codes as a multi-valued
 *      user attribute, written by the IAM service via Keycloak's Admin
 *      REST API whenever a grant changes. This is the common case.
 *   2. CLIENT token (client_credentials, no reader involved — see the
 *      earlier discussion of why a service token has no reader behind
 *      it) — journal_codes as a CLIENT attribute instead, provisioned
 *      directly on the client's own config (e.g. a content-sync service
 *      scoped to only certain journals). Client attributes in Keycloak
 *      are single-valued strings, not lists like user attributes, so
 *      this one is stored comma-separated and split here.
 *
 * Checked in that order because a client_credentials grant's synthetic
 * service-account "user" could theoretically also carry the user-shaped
 * attribute — the client attribute is the fallback, not a replacement.
 */
public class JournalCodesMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "highwire-journal-codes-mapper";
    private static final String USER_ATTRIBUTE = "journal_codes";
    private static final String CLIENT_ATTRIBUTE = "journal_codes";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(CONFIG_PROPERTIES);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(CONFIG_PROPERTIES, JournalCodesMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "HighWire Journal Codes";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds journal_codes to the token as a multi-valued claim — from the user's "
                + "journal_codes attribute for a reader token, or from the client's own "
                + "journal_codes attribute (comma-separated) when there's no user, e.g. a "
                + "client_credentials service token — so services can authorize per-journal "
                + "access without an extra call back to the IAM service.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        List<String> journalCodes = userSession.getUser().getAttributes().get(USER_ATTRIBUTE);

        if (journalCodes == null || journalCodes.isEmpty()) {
            journalCodes = fromClientAttribute(clientSessionCtx);
        }

        if (journalCodes != null && !journalCodes.isEmpty()) {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, journalCodes);
        }
    }

    private List<String> fromClientAttribute(ClientSessionContext clientSessionCtx) {
        ClientModel client = clientSessionCtx.getClientSession().getClient();
        String raw = client.getAttribute(CLIENT_ATTRIBUTE);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();
    }
}
