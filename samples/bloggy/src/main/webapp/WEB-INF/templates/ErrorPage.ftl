<#-- @ftlvariable name="error" type="java.lang.String" -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Bloggy</title>
    <link rel="stylesheet" type="text/css" href="/assets/css/normalize.css">
    <link rel="stylesheet" type="text/css" href="/assets/css/style.css">
</head>
<body>
<header>
    <a href="/"><img src="/assets/img/bloggy-logo-64.png" alt="Bloggy" title="Bloggy"/></a>
</header>
<div class="middle">
    <div class="message"></div>
    <main>
        <hr/>
        <h1>Error</h1>
        <p>An unexpected error seems to have occurred.</p>

        <code>${error!?html}</code>
    </main>
</div>
</body>
</html>
