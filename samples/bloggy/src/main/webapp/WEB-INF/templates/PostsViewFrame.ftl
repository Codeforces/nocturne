<#-- @ftlvariable name="postIds" type="java.lang.Long[]" -->

<template>
    <section class="_posts">
        <#list postIds as postId>
            <@frame name="post${postId}"/>
        </#list>
    </section>
</template>
