<#--
  HighWire branded email wrapper.

  Colors below are LITERAL hex values, not CSS custom properties — most
  email clients (Outlook desktop, many webmail clients) strip or ignore
  :root custom properties and <style> blocks with var(), so every value here
  has to be inlined directly for reliable rendering. This is the one place
  in the whole theme set where the --hw-* variable pattern used by
  login/account/admin CSS does NOT apply.

  Consequence for publisher theming: a publisher theme overriding this file
  has to copy the whole template and change the literal values (see
  ../../../publisher-example/email/html/template.ftl), not just redefine a
  few variables. Documented in themes/README.md.
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
            <td style="background-color:#6a1130; padding:20px 32px;">
              <img src="${url.resourcesUrl}/img/highwire-logo.png" alt="HighWire" width="140" style="display:block; border:0;"/>
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
