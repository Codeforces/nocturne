<#import "macros/common.ftl" as common>

<@common.page>
<div class="actions">
    <a class="addPost" rel="addPostModal" href="#">{{Write}}</a>

    <div class="addPostModal" style="display:none;">
        <div class="caption">
            {{Write post}}
        </div>

        <div class="vertical-form add-post-form">
            <table>
                <form action="12" method="post">
                    <input type="hidden" name="action" value="addPost">
                    <tr>
                        <td class="field-names">
                            {{To}}:
                        </td>
                        <td>
                            <input class="textbox" name="to" value="${to!}">
                        </td>
                    </tr>
                    <tr>
                        <td>
                            &nbsp;
                        </td>
                        <td>
                            <span class="error under error__to">&nbsp;</span>
                        </td>
                    </tr>
                    <tr>
                        <td class="field-names">
                            {{Text}}:
                        </td>
                        <td>
                            <textarea class="textarea" name="text">${text!}</textarea>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            &nbsp;
                        </td>
                        <td>
                            <span class="error under error__text">&nbsp;</span>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" class="buttons">
                            <input class="button" type="submit" value="{{Send post}}">
                        </td>
                    </tr>
                </form>
            </table>
        </div>
    </div>
</div>
<div class="caption">
    <#if mode=="all">
    <span>{{All}}</span>
    <#else>
    <@link name="UserPage" mode="all" value="{{All}}"/>
    </#if>

    <#if mode=="income">
    <span>{{Income}}</span>
    <#else>
    <@link name="UserPage" mode="income" value="{{Income}}"/>
    </#if>

    <#if mode=="outcome">
    <span>{{Outcome}}</span>
    <#else>
    <@link name="UserPage" mode="outcome" value="{{Outcome}}"/>
    </#if>
</div>
<div>
    <table class="grid">
        <thead>
        <tr>
            <th style="width:2em;">{{#}}</th>
            <th style="width:12em;">{{From}}</th>
            <th style="width:12em;">{{To}}</th>
            <th style="width:10em;">{{When}}</th>
            <th>{{Text}}</th>
        </tr>
        </thead>
        <tbody>
        <#list posts as post>
        <#if post.authorUserId=user.id>
        <#assign clazz="authored"/>
        <#else>
        <#assign clazz=""/>
        </#if>
        <tr class="${clazz!}">
            <td class="centered">${post.id}</td>
            <td class="centered"><span title="${post.authorUserName}">${post.authorUserLogin}</span></td>
            <td class="centered"><span title="${post.targetUserName}">${post.targetUserLogin}</span></td>
            <td class="centered">${post.creationTime?datetime}</td>
            <td>${post.text?html}</td>
        </tr>
        </#list>
        <#if posts?size==0>
        <tr>
            <td colspan="5">
                {{No posts}}
            </td>
        </tr>
        </#if>
        </tbody>
    </table>
</div>

<script type="text/javascript">
    $(document).ready(function() {
        $('a.addPost').smart_modal({show:function() {
            $("#sm_content .add-post-form input[name=to]").autocomplete("<@link name="SuggestUserPage"/>", {
                delay: 200,
                width: 200,
                selectFirst: false
            });
            $("#sm_content .add-post-form input[type=submit]").click(function() {
                var to = $("#sm_content .add-post-form input[name=to]").val();
                var text = $("#sm_content .add-post-form textarea[name=text]").val();

                $.post("<@link name="AddPostPage"/>", {to: to, text: text}, function(data) {
                    var size = 0;
                    for (i in data) { 
                        size++;
                    }

                    if (size > 0) {
                        $("#sm_content .add-post-form .error__to").text(data["error__to"]);
                        $("#sm_content .add-post-form .error__text").text(data["error__text"]);
                    } else {
                        document.location = "";
                    }
                }, "json");

                return false;
            });
        }});
    });
</script>
</@common.page>
