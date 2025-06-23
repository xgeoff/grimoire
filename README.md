




<project-root>/
├── pages/             ← HTML or Markdown input content
├── layouts/           ← Handlebars layouts
├── partials/          ← Optional Handlebars partials
├── data/              ← Optional page data (YAML/JSON/Groovy)
├── assets/            ← CSS/JS/images to copy as-is
└── build/
└── site/          ← Final rendered output (or configurable)


| Component     | Technology                                                   |
| ------------- | ------------------------------------------------------------ |
| Templating    | [Handlebars.java](https://github.com/jknack/handlebars.java) |
| Gradle Plugin | Written in Groovy                                            |
| Task          | `generateSite`                                               |
| Page Input    | HTML, Markdown (optional)                                    |
| Metadata      | Optional Groovy/JSON/YAML front matter                       |
| Output        | `build/site/` directory                                      |

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
