package zio2demo.web.app.html

import zio.http.template._

object HtmlErrorPages {
  def internalServerErrorPage: Html =
    HtmlContainer.wrap
      (h2("Customized", br(), "Internal Server Error"))
      (
        div(
          div(
            p("Sorry, we did not expect this failure."),
            p("This is a customized page for any errors that may happen unexpectedly"),
          ),
          a(classAttr := "next", href := "/show-not-found", "Next"),
        ),
      )

  def notFoundPage(path: String): Html =
    HtmlContainer.wrap
      (h2("Customized ", br(), "Http Not Found"))
      {
        div(
          div(
            p(s"""Sorry, your requested page "$path" does not exist."""),
            p(
              "This is a customized page for any non-existing endpoints in your application",
            ),
            a(href := "/and-another-not-found", "Try one more"),
          ),
        )
      }

  def notFoundResource(message: String): Html =
    HtmlContainer.wrap
      (h2("Customized ", br(), "Resource Not Found"))
      {
        div(
          div(
            p(message),
            p(
              "This is a customized page for any non-existing endpoints in your application",
            ),
            a(href := "/and-another-not-found", "Try one more"),
          ),
        )
      }
}