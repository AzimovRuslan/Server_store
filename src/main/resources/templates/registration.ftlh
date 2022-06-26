<#import "parts/common.ftlh" as c>
<#import "parts/loginPart.ftlh" as l>

<@c.page>
<div align="center">
    <h1>REGISTER PAGE</h1>
    <@l.login "/registration" />
    ${message!""}
</div>
</@c.page>
