<#-- @ftlvariable name="shortMode" type="java.lang.Boolean" -->
<#-- @ftlvariable name="postUser" type="bloggy.model.User" -->
<#-- @ftlvariable name="post" type="bloggy.model.Post" -->

<template>
    <article class="_post" data-postId="${post.id}">
        <section class="_title">
            <a href="<@link name="PostViewPage" postId=post.id/>">${post.title?html}</a>
        </section>
        <header>
            {{By}} ${postUser.name}
        </header>
        <div class="_text">
            <div class="_userpicFrameWrapper">
                <@frame name="userpicFrame"/>
            </div>

            <#assign shortModeMaxLength=1500/>
            <#if shortMode && (post.text?length>shortModeMaxLength)>
                <div class="_preview">
                    ${post.text?substring(0, shortModeMaxLength)?html}... <a href="#" class="_full">[view all &rarr;]</a>
                </div>
                <div class="_complete">
                    ${post.text?html}
                </div>
            <#else>
                ${post.text?html}
            </#if>
        </div>
    </article>
</template>

<script>
    $("._full").css("cursor", "pointer").click(function () {
        var $post = $(this).closest("article._post");
        $post.find("._preview").hide();
        $post.find("._complete").show();
    });
</script>

<style type="text/less">
    ._post {
        margin-bottom: @larger-gap-size;

        header {
            font-size: @smaller-font-size;
            color: @muted-color;
        }

        ._title {
            font-size: @title-font-size;

            a {
                color: @caption-color;
                text-decoration: none;
            }
        }

        ._full {
            font-size: @smaller-font-size;
        }

        ._complete {
            display: none;
        }

        ._userpicFrameWrapper {
            float: left;
        }

        ._text {
            font-family: Tahoma, Verdana, Arial, sans-serif;
            line-height: 22px;
        }

        ._text::after {
            clear: both;
            content: "";
            height: 0;
            display: block;
        }
    }

</style>
