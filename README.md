## Grimoire: A Groovy-First Static Site Generator for Gradle

Grimoire is a lightweight Gradle plugin that turns Markdown and HTML content into fully rendered static sites. Instead of juggling multiple templating engines, Grimoire leans entirely on Groovy's `SimpleTemplateEngine`. That means every layout, partial, and page is just HTML with `${groovy}` expressions, allowing you to keep markup readable while still dropping in dynamic values where needed.

Each project ships with an HTML-first layout that you can customize immediately. The default scaffold uses the following Groovy template for `layouts/default.gtpl`:

```html
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
```

Every layout renders HTML as-is, and Groovy expressions (`${...}` or `<% ... %>`) handle dynamic values such as titles, navigation, or conditional blocks. The `${content}` slot receives the rendered page body, letting you keep your pages focused on Markdown/HTML content.

### Project Structure

A typical Grimoire project looks like this:
```
<project-root>/
|-- pages/             ← Markdown or HTML pages with optional Groovy front matter
|-- layouts/           ← Groovy layout templates (`.gtpl`)
|-- partials/          ← Groovy partials (`.gtpl`) referenced via `${partial('name')}`
|-- data/              ← Optional structured data (JSON/YAML/Groovy) merged into context
|-- assets/            ← Copied to the output directory; CSS/JS can also use Groovy
|-- public/            ← Generated site (configurable)
```

| Component     | Technology / Behavior                                        |
| ------------- | ------------------------------------------------------------- |
| Templating    | Groovy `SimpleTemplateEngine` (`.gtpl`, `${}` + `<% %>`)      |
| Gradle Plugin | Groovy                                                        |
| Task          | `grim`                                                        |
| Page Input    | Markdown (`.md`) or HTML (`.html`) with Groovy expressions     |
| Metadata      | Groovy front matter (at the top of `.md`/`.html` files)       |
| Output        | `public/` directory (configurable)                            |

Text assets such as `.css`, `.js`, `.html`, `.txt`, `.svg`, `.json`, `.xml`, and source maps run through the Groovy renderer, so you can interpolate variables anywhere. Binary assets are copied as-is. SASS/SCSS files under `assets/` are compiled to CSS automatically.

### Groovy Templates & Partials

Because everything is Groovy, you can freely mix inline scripts and expressions:

```html
---
title = "Docs"
layout = "default"
---

<%
def sections = ['Intro', 'Getting Started', 'API']
%>
<h1>${title}</h1>
<ul>
<% sections.each { %>
    <li>${it}</li>
<% } %>
</ul>
```

Partials live in `partials/*.gtpl` and are available through the `partial` closure:

```html
${partial('sidebar', [navigation: navigation])}
```

Inside a partial you still have access to the full site context, including a `navigation` tree that Grimoire builds from the `pages/` directory. Each navigation item includes `type` (`file` or `directory`), `name`, `path` (without extension), and optional `children`, making it easy to loop through sections when rendering menus.

## Getting Started

### 1. Include the Plugin

```groovy
plugins {
    id 'biz.digitalindustry.grimoire' version '1.0.0'
}
```

### 2. Scaffold a Site

Generate the starter structure in the current directory:

```bash
./gradlew grim-init
```

Use `--dest` to scaffold into a subdirectory:

```bash
./gradlew grim-init --dest=docs
```

The scaffold provides Groovy layouts/partials, CSS placeholders, a favicon, and a `config.grim` ready for customization.

### 3. Build the Site

Once content is ready, run:

```bash
./gradlew grim
```

The rendered HTML lands in `public/` by default.

### 4. Preview with `grim-serve`

Start a local server (defaults to `http://localhost:8080`):

```bash
./gradlew grim-serve
```

If your site lives under a subpath (e.g., GitHub Pages project sites), set `baseUrl` in `config.grim`. Grimoire normalizes `/` to an empty string for templates, so `${baseUrl}` will either be `""` or a value like `"/docs"`. The dev server mirrors this path so local URLs match production.

### 5. Configure

`config.grim` is a regular Groovy script. Common options:

```groovy
// config.grim
sourceDir = "site"
outputDir = "public"
baseUrl = "/docs"

author = [name: 'Ada', year: 2024]

paths {
    pages    = "pages"
    layouts  = "layouts"
    partials = "partials"
    assets   = "assets"
}

server {
    port = 9000
}
```

Entirely omit blocks you don't need—the defaults cover most sites.

### Tips

- Pages use Groovy front matter for metadata: wrap the opening block with `---` lines.
- Templates can call `${partial('name')}` and pass additional bindings with a map.
- `navigation` holds the full tree of pages/sections so you can easily build menus.
- Markdown is rendered after Groovy templating so expressions work inside `.md` files.

## Development

This repository contains the Grimoire Gradle plugin. Run the full suite with:

```bash
./gradlew check
```

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.
