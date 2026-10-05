<#--
  Publisher-branded email wrapper — a full duplicate of
  ../../../hw-base/email/html/template.ftl with literal colors changed to
  match this publisher's login-screen palette (see
  ../../login/resources/css/publisher-overrides.css for the equivalent
  --hw-primary/--hw-header-bg values). Email clients need literal hex, not
  CSS custom properties, so this file can't just redefine variables the way
  the login/account/admin themes do — see hw-base's template.ftl for why.
-->
<#macro emailLayout>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
</head>
<body style="margin:0; padding:0; background-color:#f5f7fa; font-family:'Roboto', Arial, sans-serif;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f5f7fa; padding:24px 0;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" style="max-width:560px; background-color:#ffffff; border-radius:4px; overflow:hidden;" cellpadding="0" cellspacing="0">
          <tr>
            <td style="background-color:#0b2545; padding:20px 32px;">
              <img src="${url.resourcesUrl}/img/logo.svg" alt="Publisher logo" width="120" style="display:block; border:0;"/>
            </td>
          </tr>
          <tr>
            <td style="padding:32px; color:#363636; font-size:14px; line-height:1.6;">
              <#nested>
            </td>
          </tr>
          <tr>
            <td style="padding:16px 32px; border-top:1px solid #efefef; color:#666666; font-size:12px;">
              ${kcSanitize(msg("hwEmailFooter"))?no_esc}
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
</#macro>
