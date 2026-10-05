<#--
  Standard Keycloak "Terms and Conditions" required action page.
  HighWire note: for registration-time T&Cs tied to a specific publisher/RP,
  register.ftl already handles that inline (client.attributes.hwTermsUrl).
  This page is for the realm-wide Terms and Conditions required action,
  a separate Keycloak mechanism — both can be enabled independently.
-->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("termsTitle")}
    <#elseif section = "form">
        <form id="kc-terms-form" action="${url.loginAction}" method="post">
            <div class="hw-terms-text">
                ${msg("termsText")}
            </div>
            <div class="${properties.kcFormGroupClass!}" id="kc-form-buttons">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                       name="cancel" id="kc-decline" type="submit" value="${msg("doDecline")}"/>
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                       name="accept" id="kc-accept" type="submit" value="${msg("doAccept")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
