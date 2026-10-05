<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
    <#if section = "header">
        ${msg("updatePasswordTitle")}
    <#elseif section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <label for="password-new" class="${properties.kcLabelClass!}">${msg("passwordNew")} <span class="hw-required" aria-hidden="true">*</span></label>
                <input type="password" id="password-new" class="${properties.kcInputClass!}" name="password-new" autofocus autocomplete="new-password" required aria-required="true"/>
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
            <div class="${properties.kcFormGroupClass!}" id="kc-form-buttons">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                       type="submit" value="${msg("doSubmit")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
