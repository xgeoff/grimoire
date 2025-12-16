<!DOCTYPE html>
<html>
<head>
    <!-- Basic Page Needs -->
    <meta charset="utf-8">
    <title>${title ?: 'Grimoire Site'}</title>
    <meta name="description" content="${description ?: ''}">
    <meta name="author" content="${author?.name ?: ''}">

    <!-- Mobile Specific Metas -->
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <!-- FONT -->
    <link href="//fonts.googleapis.com/css?family=Raleway:400,300,600" rel="stylesheet" type="text/css">

    <!-- CSS -->
    <link rel="stylesheet" href="css/normalize.css">
    <link rel="stylesheet" href="css/skeleton.css">
    <link rel="stylesheet" href="css/style.css">

    <!-- Favicon -->
    <link rel="icon" type="image/png" href="images/favicon-32.png">
</head>
<body>
<div class="shell">
    ${partial('sidebar')}
    <main class="container prose">
${content}
    </main>
</div>
</body>
</html>
