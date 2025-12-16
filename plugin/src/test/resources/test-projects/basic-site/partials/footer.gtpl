<%
def currentAuthor = author
def globalAuthor = site?.author

Closure resolveName = {
    if (it instanceof Map) {
        return it.name ?: ''
    }
    if (it instanceof CharSequence) {
        return it.toString()
    }
    return ''
}

Closure resolveYear = {
    if (it instanceof Map) {
        return it.year ?: ''
    }
    return ''
}

def authorName = resolveName(currentAuthor)
if (!authorName) {
    authorName = resolveName(globalAuthor)
}

def authorYear = resolveYear(currentAuthor)
if (!authorYear) {
    authorYear = resolveYear(globalAuthor)
}
if (!authorYear) {
    authorYear = new Date().format('yyyy')
}
%>
<footer class="site-footer">
    <p>&copy; ${authorYear} ${authorName?.toUpperCase()}</p>
    <p>Generated with Grimoire</p>
    <% if (baseUrl) { %>
        <p><a href="${baseUrl}/privacy">Privacy Policy</a></p>
    <% } %>
</footer>
