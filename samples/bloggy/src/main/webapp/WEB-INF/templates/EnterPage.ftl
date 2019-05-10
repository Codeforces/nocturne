<#-- @ftlvariable name="password" type="java.lang.String" -->
<#-- @ftlvariable name="login" type="java.lang.String" -->
<#-- @ftlvariable name="error__login" type="java.lang.String" -->
<#-- @ftlvariable name="error__password" type="java.lang.String" -->
<#import "common.ftl" as c>

<template>
<@c.page>
<div class="form-box _enter-box">
    <div class="header">{{Enter}}</div>
    <div class="body">
        <form method="post" action="<@link name="EnterPage"/>">
            <input type="hidden" name="action" value="enter">
            <div class="field">
                <div class="name">
                    <label for="login">{{Login}}</label>
                </div>
                <div class="value">
                    <input id="login" name="login" value="${login!?html}"/>
                </div>
                <@c.errorRow value="${error__login!}"/>
            </div>
            <div class="field">
                <div class="name">
                    <label for="password">{{Password}}</label>
                </div>
                <div class="value">
                    <input id="password" type="password" name="password" value="${password!?html}"/>
                </div>
                <@c.errorRow value="${error__password!}"/>
            </div>
            <div class="button-field">
                <input type="submit" value="{{Enter}}">
            </div>
        </form>
    </div>
</div>
</@c.page>
</template>

<script>
</script>

<style type="text/less">
    ._enter-box {
        margin: 3em auto;
    }
</style>
