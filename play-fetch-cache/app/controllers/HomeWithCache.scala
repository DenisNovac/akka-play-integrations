package controllers

import cats.effect.{ContextShift, IO, Timer}
import controllers.cachefetch.CaffeineFetchModule
import controllers.datasources.ToStringDataSource
import fetch.{Data, DataCache, DataSource, Fetch}
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.{BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext

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
}
