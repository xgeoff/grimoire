## Grimoire: A Simple, Scriptable Static Site Generator for Gradle

Grimoire is a lightweight and powerful static site generator built as a Gradle plugin. It's designed for developers who want to create fast, modern websites—like blogs, documentation, or portfolios—without leaving their existing build environment. By leveraging the power of Gradle and Groovy, Grimoire offers a deeply scriptable and customizable experience.

At its core, Grimoire uses the popular Handlebars.java templating engine, then runs the result through Groovy's `SimpleTemplateEngine`. This lets you mix `{{handlebars}}` helpers with `${groovy}` interpolation in the same file. Content can be written in standard Markdown or HTML, and the plugin follows a simple convention-over-configuration approach, allowing you to get started quickly with a sensible project structure while still providing the flexibility to configure paths and build processes to fit your needs.

Whether you're building a personal blog or a project documentation site, Grimoire provides the tools to generate your static content efficiently, right from your Gradle build.

### Project Structure

A typical Grimoire project follows this structure:
```
<project-root>/ 
|-- pages/             ← HTML or Markdown input content
|-- layouts/           ← Handlebars (.hbs) or Groovy (.gtpl) layouts
|-- partials/          ← Optional Handlebars (.hbs) or Groovy (.gtpl) partials
|-- data/              ← Optional page data (YAML/JSON/Groovy)
|-- assets/            ← CSS/JS/images to copy as-is
|-- public/            ← Final rendered output (or configurable)
```

| Component     | Technology                                                   |
| ------------- |--------------------------------------------------------------|
| Templating    | [Handlebars.java](https://github.com/jknack/handlebars.java) + Groovy `SimpleTemplateEngine` |
| Gradle Plugin | Written in Groovy                                            |
| Task          | `grim`                                                       |
| Page Input    | HTML, Markdown                                               |
| Metadata      | Optional Groovy  front matter                                |
| Output        | `public/` directory (configurable)                           |

| File Type                      | Default Action                  | Optional Processing           | Notes |
| ------------------------------ | ------------------------------- | ----------------------------- | ----- |
| `.css`, `.js`, `.png`, `.jpg`  | Copy as-is                      | None (default)                |       |
| `.scss`, `.sass`               | Compile to `.css`               | ✅ SASS processor              |       |
| `.hbs` or `.html` under assets | Handlebars rendering (optional) | 🔶 Only if explicitly flagged |       |
| Other binary files             | Copy as-is                      | Skip processing               |       |

| Feature            | Behavior                                              |
| ------------------ | ----------------------------------------------------- |
| Partials directory | `site/partials/` (configurable later)                 |
| File naming        | `header.hbs` → referenced as `{{> header}}`           |
| Usage              | Can be used in layouts, pages, or other partials      |
| Registration       | Automatically load all `.hbs` files from partials dir |

### Groovy Templates

After Handlebars rendering, Grimoire evaluates the result with Groovy's `SimpleTemplateEngine`. This enables `${}` interpolation and Groovy-based partials alongside Handlebars syntax.

#### Example

```html
<h1>{{title}}</h1>
<p>Hello ${title}</p>
```

#### Groovy Layouts and Partials

* Layouts may be written as `.gtpl` files. When both `default.hbs` and `default.gtpl` exist, `.hbs` is preferred.
* Groovy partials live in the same `partials/` directory but use the `.gtpl` extension.
* Include a Groovy partial with the special `partial` closure:

```html
${partial('footer', [copy: '2024'])}
```

and in `partials/footer.gtpl`:

```html
<footer>${copy}</footer>
```

The page/front‑matter context is merged with the optional map passed to `partial`, so values defined in `config.grim` or page metadata are available.


## Getting Started

The `biz.digitalindustry.grimoire` Gradle plugin helps you build static sites using Markdown or HTML templates with Handlebars-style layouts and partials.

### 1. Include the Plugin

To use the Grimoire plugin in your project, add the following to your `build.gradle`:

```groovy
plugins {
    id 'biz.digitalindustry.grimoire' version '1.0.0'
}
```

### 2. Scaffold a New Site Project

To scaffold a new project structure in the current directory, run:

```bash
./gradlew grim-init
```

This will generate a basic site structure alongside your existing files:

```
<project-root>/
├── config.grim            # Groovy-based site configuration
├── pages/                 # Markdown or HTML page files
├── layouts/               # Handlebars (.hbs) or Groovy (.gtpl) layouts
├── partials/              # Optional Handlebars or Groovy partials
└── data/                  # Optional data in JSON/YAML/Groovy
```

Use `--dest` to scaffold into a specific subdirectory:

```bash
./gradlew grim-init --dest=my-docs
```

Existing files are left untouched; the task skips files that already exist to avoid overwriting.

### 3. Build Your Site

Once your content is ready, you can generate the site using:

```bash
./gradlew grim
```

The rendered site will be output to:

```
public
```

You can change this output location via `config.grim` using the `outputDir` property.

### 4. Preview with Built-in Server

To preview your site locally:

```bash
./gradlew grim-serve
```

The server will default to `http://localhost:8080` unless configured otherwise in `config.grim`.

#### Base URL (subpath) support

If your site is deployed under a subpath (e.g., GitHub Pages project sites), set `baseUrl` in `config.grim`. The dev server will serve your site under the same subpath so local URLs match production.

Examples:

```groovy
// Root site (default)
baseUrl = "/"            // Local: http://localhost:8080/

// Subpath (either form is accepted)
baseUrl = "arden"        // Local: http://localhost:8080/arden
// or
baseUrl = "/arden"      // Local: http://localhost:8080/arden

server { port = 8080 }
```

Notes:
- For GitHub Pages project sites use `baseUrl = "/<repo-name>"`.
- For user/org root sites use `baseUrl = "/"`.
- `baseUrl` is normalized (leading slash added, trailing slash removed). Templates see an empty string when `baseUrl` is root to avoid `//path` links.
- The starter templates already prefix links with `{{@root.baseUrl}}` when present.

### 5. Customize Your Site
Grimoire reads configuration from a `config.grim` file located in the
project root. The file is a Groovy script, allowing you to tailor the
build process programmatically. Common options include tweaking
directories and the port used by the preview server:


```groovy
// config.grim
sourceDir = "docs"        // Where your content lives
outputDir = "public"      // Build destination

paths {
    pages    = "pages"    // Content pages
    layouts  = "layouts"  // Handlebars (.hbs) or Groovy (.gtpl) layouts
    partials = "partials" // Reusable snippets (.hbs/.gtpl)
    helpers  = "helpers"  // Groovy Handlebars helpers
    assets   = "assets"   // Static files
}

server {
    port = 9000           // Used by `grim-serve`
}
```

Each block is optional—omit it to use the defaults. Helpers placed in
the `helpers/` directory are automatically compiled and registered,
while partials in `partials/` are available via the familiar
`{{> partialName}}` syntax. Front matter in pages provides per-page
metadata such as titles or layouts.

### Configuration Rules
- Single source of truth: `config.grim` controls all site behavior (paths, server, etc.).
- The Gradle block only points to the config file:
  - Groovy DSL:
    - `grimoire { configFile = file("config/prod.grim") }`
- Do not duplicate configuration in `build.gradle`; set directories and options in `config.grim`.
- Typical multi‑env setup: keep `config.grim` as default, add `config/prod.grim`, and switch via the block above.

---

## Development

This repository contains the Grimoire Gradle plugin. Run the full test
suite with:

```bash
./gradlew check
```

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.
