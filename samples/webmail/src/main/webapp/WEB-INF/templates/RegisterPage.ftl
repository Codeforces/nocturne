<#import "macros/common.ftl" as common>

<@common.page>
<div class="vertical-form register-form">
    <table>
        <form action="" method="post">
            <input type="hidden" name="action" value="register">
            <tr>
                <td class="field-names">
                    {{Name}}:
                </td>
                <td>
                    <input class="textbox" name="name" value="${name!}">
                </td>
            </tr>
            <@common.subscript error="${error__name!}"/>

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
                <td class="field-names">
                    {{Confirm password}}:
                </td>
                <td>
                    <input class="textbox" type="password" name="passwordConfirmation">
                </td>
            </tr>
            <@common.subscript error="${error__passwordConfirmation!}"/>

            <tr>
                <td colspan="2" class="buttons">
                    <input class="button" type="submit" value="{{Register}}">
                </td>
            </tr>
        </form>
    </table>
    <#if error??>
        <div class="error">${error}</div>
    </#if>
</div>
</@common.page>
