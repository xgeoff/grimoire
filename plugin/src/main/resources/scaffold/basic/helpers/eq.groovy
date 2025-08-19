package scaffold.basic.helpers

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options

return { Object a, Options options ->
    a?.toString() == options.param(0)?.toString()
} as Helper
