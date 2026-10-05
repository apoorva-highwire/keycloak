<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
    <#if section = "header">
        ${msg("doLogIn")}
    <#elseif section = "form">
        <form id="kc-otp-login-form" action="${url.loginAction}" method="post">
            <#if otpLogin.userOtpCredentials?size gt 1>
                <div class="${properties.kcFormGroupClass!}">
                    <#list otpLogin.userOtpCredentials as otpCredential>
                        <label>
                            <input type="radio" name="selectedCredentialId" value="${otpCredential.id}"
                                   <#if otpCredential.id == otpLogin.selectedCredentialId>checked="checked"</#if>/>
                            ${otpCredential.userLabel}
                        </label>
                    </#list>
                </div>
            </#if>
            <div class="${properties.kcFormGroupClass!}">
                <label for="otp" class="${properties.kcLabelClass!}">${msg("loginOtpOneTime")}</label>
                <input id="otp" name="otp" autocomplete="off" type="text" class="${properties.kcInputClass!}" autofocus
                       aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>"/>
                <#if messagesPerField.existsError('totp')>
                    <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('totp'))?no_esc}</span>
                </#if>
            </div>
            <div class="${properties.kcFormGroupClass!}" id="kc-form-buttons">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                       name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
