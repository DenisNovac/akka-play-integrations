package controllers

import play.api._
import play.api.Logging
import play.api.cache.AsyncCacheApi
import play.api.libs.json.JsValue
import play.api.mvc._
import play.cache.NamedCache
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.{Concurrent, ContextShift, IO, Timer}
import fetch.{Data, DataSource, Fetch, InMemoryCache}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import com.typesafe.scalalogging.LazyLogging
import javax.inject._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject() (
    val controllerComponents: ControllerComponents,
    @NamedCache("index-cache") indexCache: AsyncCacheApi
)(implicit val ec: ExecutionContext)
    extends BaseController
    with Logging {

  implicit val timer: Timer[IO]     = IO.timer(ec)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  def index() = Action.async(parse.json) { implicit request =>
    /** Хеш реквеста = ключ в кеше */
    indexCache.getOrElseUpdate(hashRequest(request), 1.hour) {
      logger.info("Calculating OK")
      logger.info(s"New cache value: ${hashRequest(request)}")
      Future(Ok(views.html.index()))
    }
  }

  // Источник для преобразования через fetch
  val dataSource = new ToStringDataSource[IO]

  /** Source должен быть от того же экземпляра Data, от которого делается кэш */
  val toStringData: Data[Int, String] = dataSource
  val toStringSource: DataSource[IO, Int, String] = dataSource.source



  def getNumbers() = Action { implicit request =>
    /**
      * Кеширование Fetch работает в пределах одного for
      * Этот метод вызывает source дважды - в переменной t и в for
      *
      * При повторном запросе два вызова произойдут заново
      * */
    val t = Fetch(1, toStringSource)
    Fetch.run[IO](t).unsafeRunSync

    val fetchTwice: Fetch[IO, (String, String, String)] = for {
      one   <- Fetch(1, toStringSource)
      two   <- Fetch(1, toStringSource)
      three <- Fetch(1, toStringSource)
    } yield (one, two, three)

    val s: (String, String, String) = Fetch.run[IO](fetchTwice).unsafeRunSync

    Ok(s.toString())
  }

  def getNumbersCached() = Action.async { implicit request =>
    /**
      * Кеширование в caffeine. Source вызывается один раз и только при первом запросе
      */
    indexCache.getOrElseUpdate(hashRequest(request), 1.hour) {
      val fetchTwice: Fetch[IO, (String, String, String)] = for {
        one   <- Fetch(1, toStringSource)
        two   <- Fetch(1, toStringSource)
        three <- Fetch(1, toStringSource)
      } yield (one, two, three)

      val s = Fetch.run[IO](fetchTwice).unsafeRunSync
      Future(Ok(s.toString))
    }
  }



  def getNumbersCachedFetch() = Action { implicit request =>
    /**
      * Кеширование Fetch между вызовами встроенными средствами кэша
      */

    def cache: InMemoryCache[IO] = InMemoryCache.from[IO, Int, String]((toStringData,1)->"1")

    val t: Fetch[IO, String] = Fetch(1, toStringSource)
    val r: String = Fetch.run[IO](t, cache).unsafeRunSync

    val fetchTwice: Fetch[IO, (String, String, String)] = for {
      one   <- Fetch(1, toStringSource)
      two   <- Fetch(1, toStringSource)
      three <- Fetch(1, toStringSource)
    } yield (one, two, three)

    val s: (String, String, String) = Fetch.run[IO](fetchTwice, cache).unsafeRunSync

    Ok(s.toString())
  }

  /** Контрольная сумма для кеширования */
  private def hashRequest[F](request: Request[F]) =
    java.util.Base64.getEncoder.encodeToString(
      java.security.MessageDigest.getInstance("SHA1").digest(request.uri.getBytes ++ request.body.toString.getBytes)
    )

}

/**
  * Источник данных для абстрагирования в Fetch
  *
  */
class ToStringDataSource[F[_]: Concurrent] extends Data[Int, String] with LazyLogging {
  override def name: String = "Int to String"

  private def latency[F[_]: Concurrent](milis: Long): F[Unit] =
    Concurrent[F].delay(Thread.sleep(milis))
  private def toStringInstance: Data[Int, String] = this

  val source: DataSource[F, Int, String] = new DataSource[F, Int, String] {
    override def data: Data[Int, String] = toStringInstance

    override def CF: Concurrent[F] = Concurrent[F]

    override def fetch(id: Int): F[Option[String]] =
      for {
        _ <- CF.delay(logger.info(s"[${Thread.currentThread.getId}] Calculating $id"))
        _ <- latency(100)
      } yield Option(id.toString)
  }
}
