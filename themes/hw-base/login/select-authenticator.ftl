<#--
  Shown when a user has more than one way to log in (e.g. password AND
  Shibboleth/OpenAthens brokering both configured) — matches Sigma's
  "pick how to log in" step, but driven by Keycloak's own authenticator
  selection mechanism rather than Sigma's shibboleth_dest/packet dance.
-->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("loginChooseAuthenticator")}
    <#elseif section = "form">
        <form id="kc-select-credential-form" class="hw-select-authenticator" action="${url.loginAction}" method="post">
            <#list auth.authenticationSelections as authenticationSelection>
                <button type="submit" name="authenticationExecution" value="${authenticationSelection.authExecId}"
                        class="hw-access-tile hw-authenticator-option">
                    <span class="hw-authenticator-title">${msg(authenticationSelection.displayName)}</span>
                    <span class="hw-authenticator-help">${msg(authenticationSelection.helpText)}</span>
                </button>
            </#list>
        </form>
    </#if>
</@layout.registrationLayout>
