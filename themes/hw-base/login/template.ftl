<#--
  HighWire override of Keycloak's base login template.ftl.

  This is a DELIBERATE override, not a guess — extracted the real
  theme/base/login/template.ftl from the running Keycloak 26.0.8 server
  (org.keycloak.keycloak-themes-26.0.8.jar) to copy its exact macro
  signature, every <#nested ...> call, and the message-alert/locale/
  username-redisplay logic verbatim. The ONLY change is what renders inside
  #kc-header-wrapper (was just the realm display name; now the full
  marketing panel — logo, headline, tagline, "Visit Journal" button, and
  "For Authors"/"For Reviewers" cards) and a page-level footer bar added
  before </body>. Every other page (register.ftl, error.ftl, etc.) uses
  this same template.ftl and keeps working exactly as before.

  Marketing content is driven by per-CLIENT attributes (client.attributes.*)
  — the same mechanism Keycloak's own login-oauth-grant.ftl uses for
  logoUri/tosUri/policyUri, and the same one already proven working here via
  hwCustomMessage/hwTermsUrl in login.ftl/register.ftl. Each piece is
  optional and hidden if not configured, so a publisher that hasn't set them
  still gets a clean (narrower) panel instead of broken placeholders.
-->
<#import "footer.ftl" as loginFooter>
<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}"<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}" dir="${(locale.rtl)?then('rtl','ltr')}"</#if>>

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <script src="${url.resourcesPath}/js/menu-button-links.js" type="module"></script>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="module">
        import { startSessionPolling } from "${url.resourcesPath}/js/authChecker.js";

        startSessionPolling(
          "${url.ssoLoginInOtherTabsUrl?no_esc}"
        );
    </script>
</head>

<body class="${properties.kcBodyClass!}">
<div class="hw-topbar"></div>
<div class="${properties.kcLoginClass!}">
    <div id="kc-header" class="${properties.kcHeaderClass!}">
        <div id="kc-header-wrapper" class="${properties.kcHeaderWrapperClass!} hw-marketing-panel">

            <#-- realm display name stays as the accessible document title source
                 (loginTitle above); this visible headline is separate copy -->
            <h2 class="hw-marketing-headline">${msg("hwWelcomeTo", (realm.displayName!realm.name))}</h2>

            <#if client.attributes.hwTagline?? && client.attributes.hwTagline != "">
                <p class="hw-marketing-tagline">${kcSanitize(client.attributes.hwTagline)?no_esc}</p>
            <#else>
                <p class="hw-marketing-tagline">${msg("hwDefaultTagline")}</p>
            </#if>

            <#if client.attributes.hwJournalUrl?? && client.attributes.hwJournalUrl != "">
                <a class="hw-journal-link" href="${client.attributes.hwJournalUrl}" target="_blank" rel="noopener">
                    ${msg("hwVisitJournal")}
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                </a>
            </#if>

            <#if (client.attributes.hwAuthorGuidelinesUrl?? && client.attributes.hwAuthorGuidelinesUrl != "") || (client.attributes.hwReviewerGuidelinesUrl?? && client.attributes.hwReviewerGuidelinesUrl != "")>
                <div class="hw-info-cards">
                    <#if client.attributes.hwAuthorGuidelinesUrl?? && client.attributes.hwAuthorGuidelinesUrl != "">
                        <div class="hw-info-card">
                            <div class="hw-info-card-title">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                                <span>${msg("hwForAuthors")}</span>
                            </div>
                            <a href="${client.attributes.hwAuthorGuidelinesUrl}" target="_blank" rel="noopener">${msg("hwArticleGuidelines")}</a>
                            <#if client.attributes.hwAuthorDataGuidelinesUrl?? && client.attributes.hwAuthorDataGuidelinesUrl != "">
                                <a href="${client.attributes.hwAuthorDataGuidelinesUrl}" target="_blank" rel="noopener">${msg("hwDataGuidelines")}</a>
                            </#if>
                        </div>
                    </#if>
                    <#if client.attributes.hwReviewerGuidelinesUrl?? && client.attributes.hwReviewerGuidelinesUrl != "">
                        <div class="hw-info-card">
                            <div class="hw-info-card-title">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                                <span>${msg("hwForReviewers")}</span>
                            </div>
                            <a href="${client.attributes.hwReviewerGuidelinesUrl}" target="_blank" rel="noopener">${msg("hwReviewerGuidelines")}</a>
                        </div>
                    </#if>
                </div>
            <#else>
                <#-- No journal-specific attributes configured — this is a platform-level
                     login (e.g. the admin/platform realm, or a journal realm not yet set
                     up), so show what the platform itself covers instead of leaving this
                     space empty. -->
                <ul class="hw-capability-pills">
                    <li>${msg("hwCapContentWorkflow")}</li>
                    <li>${msg("hwCapAccessIdentity")}</li>
                    <li>${msg("hwCapContentDiscovery")}</li>
                    <li>${msg("hwCapContentEngagement")}</li>
                    <li>${msg("hwCapAnalytics")}</li>
                    <li>${msg("hwCapSiteAdministration")}</li>
                </ul>
            </#if>
        </div>
    </div>
    <div class="${properties.kcFormCardClass!}">
        <header class="${properties.kcFormHeaderClass!}">
            <div class="hw-card-logo"></div>
            <#if realm.internationalizationEnabled  && locale.supported?size gt 1>
                <div class="${properties.kcLocaleMainClass!}" id="kc-locale">
                    <div id="kc-locale-wrapper" class="${properties.kcLocaleWrapperClass!}">
                        <div id="kc-locale-dropdown" class="menu-button-links ${properties.kcLocaleDropDownClass!}">
                            <button tabindex="1" id="kc-current-locale-link" aria-label="${msg("languages")}" aria-haspopup="true" aria-expanded="false" aria-controls="language-switch1">${locale.current}</button>
                            <ul role="menu" tabindex="-1" aria-labelledby="kc-current-locale-link" aria-activedescendant="" id="language-switch1" class="${properties.kcLocaleListClass!}">
                                <#assign i = 1>
                                <#list locale.supported as l>
                                    <li class="${properties.kcLocaleListItemClass!}" role="none">
                                        <a role="menuitem" id="language-${i}" class="${properties.kcLocaleItemClass!}" href="${l.url}">${l.label}</a>
                                    </li>
                                    <#assign i++>
                                </#list>
                            </ul>
                        </div>
                    </div>
                </div>
            </#if>
        <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
            <#if displayRequiredFields>
                <div class="${properties.kcContentWrapperClass!}">
                    <div class="${properties.kcLabelWrapperClass!} subtitle">
                        <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                    </div>
                    <div class="col-md-10">
                        <h1 id="kc-page-title"><#nested "header"></h1>
                    </div>
                </div>
            <#else>
                <h1 id="kc-page-title"><#nested "header"></h1>
            </#if>
        <#else>
            <#if displayRequiredFields>
                <div class="${properties.kcContentWrapperClass!}">
                    <div class="${properties.kcLabelWrapperClass!} subtitle">
                        <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                    </div>
                    <div class="col-md-10">
                        <#nested "show-username">
                        <div id="kc-username" class="${properties.kcFormGroupClass!}">
                            <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                            <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">
                                <div class="kc-login-tooltip">
                                    <i class="${properties.kcResetFlowIcon!}"></i>
                                    <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                </div>
                            </a>
                        </div>
                    </div>
                </div>
            <#else>
                <#nested "show-username">
                <div id="kc-username" class="${properties.kcFormGroupClass!}">
                    <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                    <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">
                        <div class="kc-login-tooltip">
                            <i class="${properties.kcResetFlowIcon!}"></i>
                            <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                        </div>
                    </a>
                </div>
            </#if>
        </#if>
      </header>
      <div id="kc-content">
        <div id="kc-content-wrapper">

          <#-- App-initiated actions should not see warning messages about the need to complete the action -->
          <#-- during login.                                                                               -->
          <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
              <div class="alert-${message.type} ${properties.kcAlertClass!} pf-m-<#if message.type = 'error'>danger<#else>${message.type}</#if>">
                  <div class="pf-c-alert__icon">
                      <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
                      <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
                      <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
                      <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
                  </div>
                      <span class="${properties.kcAlertTitleClass!}">${kcSanitize(message.summary)?no_esc}</span>
              </div>
          </#if>

          <#nested "form">

          <#if auth?has_content && auth.showTryAnotherWayLink()>
              <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                  <div class="${properties.kcFormGroupClass!}">
                      <input type="hidden" name="tryAnotherWay" value="on"/>
                      <a href="#" id="try-another-way"
                         onclick="document.forms['kc-select-try-another-way-form'].requestSubmit();return false;">${msg("doTryAnotherWay")}</a>
                  </div>
              </form>
          </#if>

          <#nested "socialProviders">

          <#if displayInfo>
              <div id="kc-info" class="${properties.kcSignUpClass!}">
                  <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                      <#nested "info">
                  </div>
              </div>
          </#if>
        </div>
      </div>

      <@loginFooter.content/>
    </div>
  </div>

  <div class="hw-page-footer">
    <div class="hw-page-footer-links">
      <#if client.attributes.hwFeedbackUrl?? && client.attributes.hwFeedbackUrl != ""><a href="${client.attributes.hwFeedbackUrl}" target="_blank" rel="noopener">${msg("hwFeedback")}</a></#if>
      <#if client.attributes.hwHelpUrl?? && client.attributes.hwHelpUrl != ""><a href="${client.attributes.hwHelpUrl}" target="_blank" rel="noopener">${msg("hwHelp")}</a></#if>
      <#if client.attributes.hwPrivacyUrl?? && client.attributes.hwPrivacyUrl != ""><a href="${client.attributes.hwPrivacyUrl}" target="_blank" rel="noopener">${msg("hwPrivacyPolicy")}</a></#if>
      <#if client.attributes.hwTermsUrl?? && client.attributes.hwTermsUrl != ""><a href="${client.attributes.hwTermsUrl}" target="_blank" rel="noopener">${msg("hwTermsLinkText")?cap_first}</a></#if>
    </div>
    <div class="hw-page-footer-copy">${msg("hwFooterCopyright", .now?string("yyyy"))}</div>
  </div>
</body>
</html>
</#macro>
