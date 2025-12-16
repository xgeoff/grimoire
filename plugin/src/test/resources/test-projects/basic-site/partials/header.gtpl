<header>
    <h1>${title}</h1>
    <p>Welcome to Grimoire</p>
    <nav>
        <ul>
        <% (navigation ?: []).findAll { it.type == 'file' }.each { page -> %>
            <li><a href="${(baseUrl ?: '')}/${page.path}.html">${page.name}</a></li>
        <% } %>
        </ul>
    </nav>
</header>
