package scaffold.basic.helpers

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options

return { Object tagArg, Options options ->
    String tag = String.valueOf(tagArg ?: "").trim()
    String cls = (opts.hash("class") ?: "").toString().trim()
    String classAttr = cls ? " class=\"${handlebars.escapeExpression(cls)}\"" : ""

    new Handlebars.SafeString("<${tag}${classAttr}>")
} as Helper
