package zio2demo.web.app.html

import zio.http.template._

object HtmlPages {
  def homePage: Html =
    HtmlContainer.wrap
      (
        h2(styleAttr := "border: none;", "You're running on ZIO Http!"),
      )
      {
        div(
          p(
            """This starter project may help you getting started with your own web application, zio-http-start:0.0.1 """,
          ),
          p("""You can easily inspect and modify the code, it's only a single file"""),
          h3("Project examples"),
          ul(
            li(a(href := "/sbt-revolver", "Hot-Reload changes with SBT-Revolver")),
            li(a(href := "/docker", "Dockerize this application")),
            li(a(href := "/webapi-with-endpoints", """WebAPI with Endpoints""")),
            li(
              a(href := "/show-internal-server-error", """Customized "Internal Server Error""""),
            ),
            li(a(href := "/show-not-found", """Customized "Not Found"""")),
          ),
          a(classAttr := "next", href := "/sbt-revolver", "Start"),
        )
      }

  def sbtRevolverPage: Html =
    HtmlContainer.wrap
      (h2("Hot-Reload changes with SBT-Revolver"))
      {
        div(
          ul(
            li(h4("Start your app with SBT-Revolver")),
            li(pre("sbt:zio-http-start> ~reStart")),
          ),
          p("""Your app will be reloaded whenever your source code changes"""),
          p(
            """Besides SBT-Revolver this starter project also contains other useful """,
            a(
              href := "https://zio.dev/zio-http/installation#hot-reload-changes-watch-mode",
              "SBT plugins",
            ),
            """.""",
          ),
          a(classAttr := "next", href := "/docker", "Next"),
        )
      }

  def dockerPage: Html =
    HtmlContainer.wrap
      (h2("Dockerize this application"))
      {
        div(
          ul(
            li(h4("Build a local docker image directly from SBT")),
            li(pre("sbt:zio-http-start> Docker / publishLocal")),
            li(h4("Then run a container")),
            li(pre("$ docker run -p 8080:8080 zio-http-start:0.0.1")),
            li(
              h4(
                """Then point a browser at """,
                a(href := "http://localhost:8080", "http://localhost:8080"),
              ),
            ),
            li(h4("Customize your Dockerfile")),
            li(
              p(
                s"""Lookup """,
                a(
                  href := "https://sbt-native-packager.readthedocs.io/en/stable/formats/docker.html#settings",
                  "the docs",
                ),
                """ for the SBT Native Packager Plugin and learn how to customize your Dockerfile""",
              ),
            ),
          ),
          a(classAttr := "next", href := "/webapi-with-endpoints", "Next"),
        )
      }

  def webApiPage: Html =
    HtmlContainer.wrap
      (h2("WebAPI with Endpoints"))
      {
        div(
          ul(
            li(h4("Describe your API's endpoints")),
            li(pre("""
              Endpoint
              .get("api" / "users" / int("userId"))
              .query(queryBool("show-details"))
              .out[String]
              """)),
            li(
              p(
                s"""Endpoints serve as a single source of truth for your whole Http API""",
              ),
              p(
                """Learn more about """,
                a(
                  href := "https://github.com/zio/zio-http/blob/main/zio-http-example/src/main/scala/example/EndpointExamples.scala",
                  "ZIO Endpoints",
                ),
              ),
            ),
          ),
          a(classAttr := "next", href := "/show-internal-server-error", "Next"),
        )
      }
}
