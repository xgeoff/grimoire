package scaffold.basic.helpers

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options

return { Object path, Options options ->
    def p = path?.toString() ?: ""
    if (p.toLowerCase().endsWith('.html')) {
        return p
    }
    p = p.replaceFirst(/\.[^\/]+$/, '')
    return p + '.html'
} as Helper
