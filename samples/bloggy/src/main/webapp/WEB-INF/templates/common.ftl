<#-- @ftlvariable name="message" type="java.lang.String" -->
<#-- @ftlvariable name="now" type="java.util.Date" -->
<#-- @ftlvariable name="user" type="bloggy.model.User" -->
<#-- @ftlvariable name="locale" type="java.lang.String" -->
<#macro page>
<!DOCTYPE html>
<html lang="${locale}">
<head>
    <meta charset="UTF-8">
    <title>Bloggy</title>
    <link rel="stylesheet" type="text/css" href="/assets/css/normalize.css">
    <link rel="stylesheet" type="text/css" href="/assets/css/style.css">
    <link rel="stylesheet" type="text/css" href="/assets/css/form.css">
    <link rel="stylesheet" type="text/css" href="/assets/css/datatable.css">
    <link rel=icon href=/assets/img/favicon.png sizes="16x16" type="image/png">
    <script src="/assets/js/jquery-3.4.1.js"></script>
    <script src="/assets/js/app.js"></script>
</head>
<body>
<header>
    <a href="<@link name="IndexPage"/>"><img src="/assets/img/bloggy-logo-64.png" alt="Bloggy" title="Bloggy"/></a>
    <div class="languages">
        <a href="?locale=en"><img src="/assets/img/gb.png" alt="In English" title="In English"/></a>
        <a href="?locale=ru"><img src="/assets/img/ru.png" alt="{{In Russian}}" title="{{In Russian}}"/></a>
    </div>
    <div class="enter-or-register-box">
        <#if user??>
            ${user.name}
            |
            <a class="logout" href="<@link name="LogoutDataPage"/>">{{Logout}}</a>
        <#else>
            <a href="<@link name="EnterPage"/>">{{Enter}}</a>
        </#if>
    </div>
    <nav>
        <ul>
            <li><a href="<@link name="IndexPage"/>">{{Home}}</a></li>
            <li><a href="<@link name="MyPostsPage"/>">{{My}}</a></li>
        </ul>
    </nav>
</header>
<div class="middle">
    <div class="message"></div>
    <main>
        <#nested/>
    </main>
</div>
<footer>
    <a href="<@link name="IndexPage"/>">Bloggy</a> &copy; 2009-2023 by Mike Mirzayanov
    <div class="time">${now?string('yyyy-MM-dd HH:mm:ss')}</div>
</footer>
<script>
    $.ajaxSetup({scriptCharset: "utf-8", contentType: "application/x-www-form-urlencoded; charset=UTF-8"});

    ajax = function(href, params, handler) {
        $.post(href, params, function(response) {
            if (response.success === "true") {
                handler(response);
            } else {
                alert(response.error || "{{Oops, something went wrong. Please, try again later.}}");
            }
        }, "json");
    };

    $(function () {
        var message = getMessage();
        if (!message) {
            <#if message??>
                message = "${message?js_string}";
            </#if>
        }
        if (message) {
            removeMessage();
            var $message = $(".middle .message").text(message).show();
            setTimeout(function () {
                $message.fadeOut("slow");
            }, 5000);
        }

        $(".logout").click(function() {
            ajax($(this).attr("href"), {action: "logout"}, function () {
                document.location.href = "<@link name="EnterPage"/>";
            });
            return false;
        });
    })
</script>
</body>
</html>
</#macro>

<#macro errorRow value="" default="" clazz="">
    <#if value?? && (value?length > 0)>
        <div class="subscription-row error ${clazz!}">${value?html}</div>
    <#elseif default?? && (default?length > 0)>
        <div class="subscription-row ${clazz!}">${default?html}</div>
    </#if>
</#macro>
