<#setting url_escaping_charset='UTF-8'>
<#macro page>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Expires" content="-1">
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8">
    <title>${pageTitle} - Webmail</title>
    <link href=${home}/favicon.ico" rel="icon" type="image/x-icon" />
    <link rel="stylesheet" href="${home}/css/clear.css" type="text/css"/>
    <link rel="stylesheet" href="${home}/css/style.css" type="text/css"/>
    <#list css as file>
    <link rel="stylesheet" href="${home}/${file}"/>
    </#list>
    <script type="text/javascript" src="${home}/js/jquery-1.3.2.min.js"></script>
    <#list js as file>
    <script type="text/javascript" src="${home}/${file}"></script>
    </#list>
</head>
<body>
<div id="header">
    <@frame name="menuFrame"/>
    <a href="${home}" title="{{Webmail home page}}">
        <img src="${home}/images/logo.png" width="95" height="56"/>
    </a>
</div>
<div id="content">
    <#nested>
</div>
<div id="footer">
    <@caption params=["http://code.google.com/p/nocturne"]>
    Webmail sample application for <a href="{0}">Nocturne</a> framework
    </@caption>
</div>
</body>
</html>
</#macro>

<#macro errorLabel text = "">
<#if text?? && (text?length > 0)>
<div class="error">${text!?html}</div>
</#if>
</#macro>

<#macro subscript error = "" hint = "" clazz = "under">
<#if (error?? && (error?length > 0)) || (hint?? && (hint?length > 0))>
<tr>
    <td>&nbsp;</td>
    <td>
        <div class="${clazz}">
            <#if error?? && (error?length > 0)>
            <@errorLabel text=error/>
            <#else>
            ${hint}
            </#if>
        </div>
    </td>
</tr>
</#if>
</#macro>
