package controllers

/** https://tapir.softwaremill.com/en/latest/server/play.html */
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import converter.HttpToPlayRoutesConverter
import javax.inject.Inject
import sttp.tapir._
import sttp.tapir.server.play._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

class TapirRouter @Inject() (implicit system: ActorSystem, mat: Materializer, prefix: String = "")
    extends SimpleRouter {

  val helloRoute: Endpoint[Unit, Unit, String, Nothing] =
    endpoint.get
      .in("hello")
      .out(plainBody[String])

  def helloLogic: Future[Either[Unit, String]] =
    Future(Right[Unit, String]("Hello"))

  /** Сразу получаем плеевский роут напрямую */
  val helloPlayRoute: Routes =
    helloRoute.toRoute(_ => helloLogic)

  // Можно уже тянуть сюда
  //override def routes: Routes = helloPlayRoute

  /** OpenAPI дока */
  private val openApi: OpenAPI = List(
    helloRoute
  ).toOpenAPI(
    "Tapir Play OpenAPI",
    "0.0.1"
  )

  /** Роут Akka Http */
  val openApiRoute: Route = new SwaggerAkka(openApi.toYaml, "api").routes

  /** Конвертация в Play Routes */
  val converter = new HttpToPlayRoutesConverter(mat, prefix)

  val openApiRoutes: Routes = converter.convert(openApiRoute)

  override def routes: Routes = openApiRoutes
}
