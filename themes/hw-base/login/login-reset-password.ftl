<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <label for="username" class="${properties.kcLabelClass!}">
                    <#if !realm.loginWithEmailAllowed>${msg("username")}
                    <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                    <#else>${msg("email")}</#if>
                    <span class="hw-required" aria-hidden="true">*</span>
                </label>
                <input type="text" id="username" class="${properties.kcInputClass!}" name="username" autofocus value="${(auth.attemptedUsername!'')}" required aria-required="true"/>
                <#if messagesPerField.existsError('username')>
                    <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
                </#if>
            </div>
            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                <div id="kc-form-options">
                    <span><a href="${url.loginUrl}">${msg("backToLogin")}</a></span>
                </div>
                <div id="kc-form-buttons">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doSubmit")}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
