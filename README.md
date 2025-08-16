## Grimoire: A Simple, Scriptable Static Site Generator for Gradle

Grimoire is a lightweight and powerful static site generator built as a Gradle plugin. It's designed for developers who want to create fast, modern websites—like blogs, documentation, or portfolios—without leaving their existing build environment. By leveraging the power of Gradle and Groovy, Grimoire offers a deeply scriptable and customizable experience.

At its core, Grimoire uses the popular Handlebars.java templating engine and supports content written in standard Markdown or HTML. It follows a simple convention-over-configuration approach, allowing you to get started quickly with a sensible project structure, while still providing the flexibility to configure paths and build processes to fit your needs.

Whether you're building a personal blog or a project documentation site, Grimoire provides the tools to generate your static content efficiently, right from your Gradle build.

### Project Structure

A typical Grimoire project follows this structure:
```
<project-root>/ 
|-- pages/             ← HTML or Markdown input content
|-- layouts/           ← Handlebars layouts
|-- partials/          ← Optional Handlebars partials
|-- data/              ← Optional page data (YAML/JSON/Groovy)
|-- assets/            ← CSS/JS/images to copy as-is
|-- public/            ← Final rendered output (or configurable)
```

| Component     | Technology                                                   |
| ------------- |--------------------------------------------------------------|
| Templating    | [Handlebars.java](https://github.com/jknack/handlebars.java) |
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
├── layouts/               # Handlebars layouts
├── partials/              # Optional Handlebars partials
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

### 5. Customize Your Site

