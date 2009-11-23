<#import "macros/common.ftl" as common>

<@common.page>
<div class="vertical-form login-form">
    <table>
        <form action="" method="post">
            <input type="hidden" name="action" value="login">
            <tr>
                <td class="field-names">
                    {{Login}}:
                </td>
                <td>
                    <input class="textbox" name="login" value="${login!}">
                </td>
            </tr>
            <@common.subscript error="${error__login!}"/>
            <tr>
                <td class="field-names">
                    {{Password}}:
                </td>
                <td>
                    <input class="textbox" type="password" name="password">
                </td>
            </tr>
            <@common.subscript error="${error__password!}"/>
            <tr>
                <td colspan="2" class="buttons">
                    <input class="button" type="submit" value="{{Enter}}">
                </td>
            </tr>
        </form>
    </table>
</div>
</@common.page>
