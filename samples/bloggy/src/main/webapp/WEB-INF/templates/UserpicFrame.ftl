<#-- @ftlvariable name="userpicUser" type="bloggy.model.User" -->
<#-- @ftlvariable name="userpicIndex" type="java.lang.Integer" -->

<template>
    <div class="_userpic">
        <div>
            <img src="/assets/img/user${userpicIndex}.png" alt="${userpicUser.login}" title="${userpicUser.login}"/>
            <div class="_login">${userpicUser.login}</div>
        </div>
    </div>
</template>

<script>
    $("._full").css("cursor", "pointer").click(function () {
        var $post = $(this).closest("article._post");
        $post.find("._preview").hide();
        $post.find("._complete").show();
    });
</script>

<style type="text/less">
    ._userpic {
        text-align: center;
        margin: @smallest-gap-size;

        img {
            display: block;
            width: 64px;
        }

        ._login {
            font-weight: bold;
            font-family: "Arial Narrow", sans-serif;
        }
    }
</style>
