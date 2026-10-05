<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=false; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">

        <#-- HighWire: optional per-client custom message banner, equivalent to Sigma's custom_message param (OIDC Integration with SAMS Sigma-v5.0.2.pdf §3.4.1 authorize request params) -->
        <#if client.attributes.hwCustomMessage?? && client.attributes.hwCustomMessage != "">
            <p class="hw-custom-message">${kcSanitize(client.attributes.hwCustomMessage)?no_esc}</p>
        </#if>

        <div id="kc-form" <#if realm.password && social.providers??>class="${properties.kcContentWrapperClass!}"</#if>>
          <div id="kc-form-wrapper" <#if realm.password && social.providers??>class="${properties.kcFormSocialAccountContentClass!} ${properties.kcFormSocialAccountClass!}"</#if>>
            <#if realm.password>
                <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                    <#if !usernameHidden??>
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="username" class="${properties.kcLabelClass!}">
                                <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                                <span class="hw-required" aria-hidden="true">*</span>
                            </label>
                            <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username"
                                   value="${(login.username!'')}" type="text" autofocus autocomplete="off"
                                   required aria-required="true"
                                   aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"/>
                            <#if messagesPerField.existsError('username','password')>
                                <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                                </span>
                            </#if>
                        </div>
                    </#if>

                    <div class="${properties.kcFormGroupClass!}">
                        <label for="password" class="${properties.kcLabelClass!}">
                            ${msg("password")}
                            <span class="hw-required" aria-hidden="true">*</span>
                        </label>
                        <div class="hw-password-field">
                            <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off"
                                   required aria-required="true"
                                   aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"/>
                            <button type="button" class="hw-password-toggle" aria-label="${msg("hwShowPassword")}"
                                    onclick="(function(btn){
                                        var input = document.getElementById('password');
                                        var showing = input.type === 'text';
                                        input.type = showing ? 'password' : 'text';
                                        btn.setAttribute('aria-label', showing ? '${msg("hwShowPassword")}' : '${msg("hwHidePassword")}');
                                        btn.classList.toggle('hw-password-toggle--visible', !showing);
                                    })(this)">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z"/><circle cx="12" cy="12" r="3"/></svg>
                            </button>
                        </div>
                    </div>

                    <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                        <div id="kc-form-options">
                            <#if realm.rememberMe && !usernameHidden??>
                                <div class="checkbox">
                                    <label>
                                        <#if login.rememberMe??>
                                            <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                                        <#else>
                                            <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                                        </#if>
                                    </label>
                                </div>
                            </#if>
                        </div>
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                            <#if realm.resetPasswordAllowed>
                                <span><a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                            </#if>
                        </div>
                    </div>

                    <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                        <input type="hidden" id="id-hidden-input" name="credentialId"
                               <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                        <input tabindex="4"
                               class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                    </div>

                    <#if realm.registrationAllowed && !registrationDisabled??>
                        <div class="${properties.kcFormGroupClass!}">
                            <a tabindex="6" href="${url.registrationUrl}" class="hw-secondary-button">${msg("hwCreateAccount")}</a>
                        </div>
                    </#if>
                </form>
            </#if>
          </div>

          <#-- Institution SSO — real Keycloak identity brokering, not a hardcoded link.
               Shows only once at least one IdP is configured on the realm. -->
          <#if realm.identityProviders?? && (((realm.identityProviders?size)!0) gt 0)>
              <div id="hw-access-methods" class="hw-access-methods">
                <a class="hw-access-tile" href="${url.loginUrl}&kc_idp_hint=shibboleth">${msg("hwLogInViaInstitution")}</a>
              </div>
          </#if>
        </div>

    <#elseif section = "socialProviders">
        <#if realm.password && social?? && social.providers?has_content>
            <div id="kc-social-providers" class="hw-social-providers">
                <div class="hw-social-divider"><span>${msg("hwOrContinueWith")}</span></div>
                <ul class="hw-social-list">
                    <#list social.providers as p>
                        <li>
                            <a id="social-${p.alias}" class="hw-social-button" type="button" href="${p.loginUrl}">
                                <#if p.iconClasses?has_content><i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i></#if>
                                <span>${kcSanitize(p.displayName)?no_esc}</span>
                            </a>
                        </li>
                    </#list>
                </ul>
            </div>
        </#if>
    <#elseif section = "info">
    </#if>
</@layout.registrationLayout>
