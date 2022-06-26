<#import "parts/common.ftlh" as c>
<#import "parts/loginPart.ftlh" as l>

<@c.page>
    <div align=center>
        <h1>Welcome to clothing and shoes store</h1>
    </div>
    <div align="center">
        <h1>LOGIN PAGE</h1>
        <@l.login "/login" />
        <a href="/registration" id="registration">Register</a>
    </div>
</@c.page>
