package zio2demo.web.app.html

import zio.http.template._

object HtmlContainer {
  def wrap(contentTitle: zio.http.template.Html)(content: zio.http.template.Html): Html =
    html(
      head(
        title("ZIO Http"),
        link(relAttr     := "stylesheet", href := "/assets/styles.css"),
        meta(charsetAttr := "utf-8"),
      ),
      body(
        header(
          a(href := "/", img(srcAttr := "/assets/zio.png")),
          nav(
            ul(
              li(a(href := "/", "Homepage")),
              li(a(href := "https://zio.dev/zio-http/", "Documentation")),
              li(a(href := "https://www.github.com/zio/zio-http/", "Github")),
              li(
                a(
                  href := "https://github.com/zio/zio-http/tree/main/zio-http-example/src/main/scala/example",
                  "Github Examples",
                ),
              ),
            ),
          ),
        ),
        main(
          div(
            contentTitle,
            content,
          ),
        ),
      ),
    )
}