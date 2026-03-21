<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>${page.title ?: 'Page'}</title>
</head>
<body>
<div>Sidebar title: ${page.meta.sidebar?.title ?: 'none'}</div>
<div>Sections link: ${page.meta.sidebar?.sections?.getAt(0)?.links?.getAt(0)?.label ?: 'missing'}</div>
<div>Page path: ${page.path}</div>
<div>Page url: ${page.url}</div>
<div>Tags: ${page.meta.tags?.join(', ') ?: 'untagged'}</div>
<article>${content}</article>
</body>
</html>
