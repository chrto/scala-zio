package zio2demo.web.app

import zio.http.{Routes, Method, Handler, Request, Path, Response, Status}
import zio.http.trailing

object WebAppAssets {
  import zio2demo.web.app.html.HtmlErrorPages

  def make = Routes(
    Method.GET / "assets" / trailing ->
      Handler.param[(Path, Request)] { case (path, _) => path }
        .flatMap((path: Path) => Handler.getResourceAsFile(s"${path.encode}"))
        .tapErrorZIO(e => zio.Console.printLine(s"Error in getting file: ${e.getMessage}, ${e.getClass}"))
        .flatMap{(file: java.io.File) =>
          Handler.param[(Path, Request)]{case (_, req) => req}
          >>> (
            file.isFile match
              case true => Handler.fromFile(file)
              case false => Handler.notFound
            )
          }
  )
  .handleError{
    case e: java.io.FileNotFoundException => Response.html(
      HtmlErrorPages.notFoundResource(e.getMessage),
      Status.NotFound,
    )
     case e: java.lang.IllegalArgumentException => Response.html(
      HtmlErrorPages.notFoundResource(e.getMessage),
      Status.NotFound,
    )
    case e => Response.html(
      HtmlErrorPages.internalServerErrorPage,
      Status.InternalServerError,
    )
  }
}