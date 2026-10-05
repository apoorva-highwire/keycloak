<#--
  Shown on first login via a brokered IdP (Shibboleth/OpenAthens/SAML/OIDC —
  the mechanisms NEW-AUTH-AUTHZ-KEYCLOAK-PLAN.md treats as "native via
  identity brokering"), letting the reader confirm the profile info the
  institution's IdP asserted before Keycloak creates/links the account.
-->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("loginIdpReviewProfileTitle")}
    <#elseif section = "form">
        <form id="kc-idp-review-profile-form" action="${url.loginAction}" method="post">
            <#list profile.attributes as attribute>
                <div class="${properties.kcFormGroupClass!}">
                    <label for="${attribute.name}" class="${properties.kcLabelClass!}">${advancedMsg(attribute.displayName!attribute.name)}</label>
                    <input type="text" id="${attribute.name}" name="${attribute.name}" class="${properties.kcInputClass!}"
                           value="${(attribute.value!'')}" <#if attribute.readOnly>readonly</#if>/>
                    <#if messagesPerField.existsError(attribute.name)>
                        <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get(attribute.name))?no_esc}</span>
                    </#if>
                </div>
            </#list>
            <div class="${properties.kcFormGroupClass!}" id="kc-form-buttons">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                       type="submit" value="${msg("doSubmit")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
