package zio2demo.web.app

import zio.http.{Routes, Method, Handler, RoutePattern, Path, Request, Response, Status}
import zio.http.template._

object WebApp {
  import zio2demo.web.app.html.{HtmlPages, HtmlErrorPages}

  // Web Application
  def make: Routes[Any, Nothing] = Routes(
      Method.GET / ""                       -> Handler.html(HtmlPages.homePage),
      Method.GET / "sbt-revolver"           -> Handler.html(HtmlPages.sbtRevolverPage),
      Method.GET / "docker"                 -> Handler.html(HtmlPages.dockerPage),
      Method.GET / "webapi-with-endpoints"  -> Handler.html(HtmlPages.webApiPage),
      RoutePattern.any                      -> Handler
        .param[(Path, Request)](_._1)
        .map(_.encode)
        .map(HtmlErrorPages.notFoundPage)
        .map(Response.html(_, Status.NotFound))
  )
}