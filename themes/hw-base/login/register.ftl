<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>
    <#if section = "header">
        ${msg("registerTitle")}
    <#elseif section = "form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post">
            <#if !realm.registrationEmailAsUsername>
                <div class="${properties.kcFormGroupClass!}">
                    <label for="username" class="${properties.kcLabelClass!}">${msg("username")} <span class="hw-required" aria-hidden="true">*</span></label>
                    <input type="text" id="username" class="${properties.kcInputClass!}" name="username"
                           value="${(register.formData.username!'')}" autocomplete="username" required aria-required="true"/>
                    <#if messagesPerField.existsError('username')>
                        <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
                    </#if>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <label for="email" class="${properties.kcLabelClass!}">${msg("email")} <span class="hw-required" aria-hidden="true">*</span></label>
                <input type="email" id="email" class="${properties.kcInputClass!}" name="email"
                       value="${(register.formData.email!'')}" autocomplete="email" required aria-required="true"/>
                <#if messagesPerField.existsError('email')>
                    <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('email'))?no_esc}</span>
                </#if>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <label for="password" class="${properties.kcLabelClass!}">${msg("password")} <span class="hw-required" aria-hidden="true">*</span></label>
                <input type="password" id="password" class="${properties.kcInputClass!}" name="password" autocomplete="new-password" required aria-required="true"/>
                <#if messagesPerField.existsError('password')>
                    <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('password'))?no_esc}</span>
                </#if>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")} <span class="hw-required" aria-hidden="true">*</span></label>
                <input type="password" id="password-confirm" class="${properties.kcInputClass!}" name="password-confirm" autocomplete="new-password" required aria-required="true"/>
                <#if messagesPerField.existsError('password-confirm')>
                    <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</span>
                </#if>
            </div>

            <#-- HighWire: registration terms & conditions, matching Sigma's forced-accept T&Cs text
                 that links to an external publisher-hosted page (§3.8.2.2 of the Sigma OIDC doc).
                 The actual link target is a per-client attribute, set by the publisher onboarding flow. -->
            <#if client.attributes.hwTermsUrl?? && client.attributes.hwTermsUrl != "">
                <div class="${properties.kcFormGroupClass!} hw-terms">
                    <label>
                        <input type="checkbox" id="hwTermsAccepted" name="hwTermsAccepted" required/>
                        ${msg("hwAcceptTerms")} <a href="${client.attributes.hwTermsUrl}" target="_blank" rel="noopener">${msg("hwTermsLinkText")}</a>
                    </label>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doRegister")}"/>
                </div>
            </div>
        </form>
    <#elseif section = "info">
        <div id="kc-form-options">
            <span><a href="${url.loginUrl}">${msg("backToLogin")}</a></span>
        </div>
    </#if>
</@layout.registrationLayout>
