<div class="sidebar">
    <h1>${title ?: 'Grimoire Site'}</h1>
    <ul>
    <% (navigation ?: []).each { item -> %>
        ${partial('sidebarItem', [item: item])}
    <% } %>
    </ul>
</div>
