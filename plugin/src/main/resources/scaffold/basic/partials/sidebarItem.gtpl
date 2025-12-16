<% def navItem = item ?: [:] %>
<% if (navItem.type == 'directory') { %>
<li class="sidebar__group">
    <span>${navItem.name}</span>
    <ul>
    <% (navItem.children ?: []).each { child -> %>
        ${partial('sidebarItem', [item: child])}
    <% } %>
    </ul>
</li>
<% } else if (navItem.type == 'file') { %>
<% def rawBase = baseUrl ?: '' %>
<% def normalizedBase = rawBase.length() > 1 && rawBase.endsWith('/') ? rawBase[0..-2] : rawBase %>
<% def href = normalizedBase ? "${normalizedBase}/${navItem.path}.html" : "/${navItem.path}.html" %>
<li class="file"><a href="${href}">${navItem.name}</a></li>
<% } %>
