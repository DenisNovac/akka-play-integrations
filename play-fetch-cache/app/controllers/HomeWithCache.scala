package controllers

import cats.effect.{ContextShift, IO, Timer}
import controllers.cachefetch.CaffeineFetchModule
import controllers.datasources.{StringToLowerSource, ToStringDataSource}
import fetch.{Data, DataCache, DataSource, Fetch}
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeWithCache @Inject() (
    val controllerComponents: ControllerComponents,
    fetchCache: CaffeineFetchModule
)(implicit val ec: ExecutionContext)
    extends BaseController
    with Logging {

  implicit val timer: Timer[IO]     = IO.timer(ec)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val str: ToStringDataSource[IO]         = new ToStringDataSource[IO]
  val data: Data[Int, String]             = str
  val source: DataSource[IO, Int, String] = str.source

  def cache: DataCache[IO] = fetchCache.cache

  def index() = Action { implicit request =>
    val f: Fetch[IO, String] = Fetch(1, source)
    val r                    = Fetch.run[IO](f, cache).unsafeRunSync
    Fetch.run[IO](f, cache).unsafeRunSync

    val f2: Fetch[IO, String] = Fetch(2, source)
    Fetch.run[IO](f2, cache).unsafeRunSync
    Fetch.run[IO](f2, cache).unsafeRunSync
    Ok(r)
  }

  val lwr: StringToLowerSource[IO]             = new StringToLowerSource[IO]
  val dataSt: Data[String, String]             = lwr
  val sourceSt: DataSource[IO, String, String] = lwr.source

  def lower = Action.async(parse.byteString) { implicit request =>
    val input = request.body.decodeString("UTF-8")
    val f: Fetch[IO, String] = Fetch(input, sourceSt)
    val r: String = Fetch.run[IO](f, cache).unsafeRunSync
    Future(Ok(r))
  }
}
