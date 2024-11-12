package zio2demo.web.app

import zio.http.{Routes}

object WebAppRoutes {
  import zio2demo.web.app.html.HtmlErrorPages
  def routes: Routes[Any, Nothing] =
    (
      WebApp.make ++
      WebAppAssets.make
    )
}