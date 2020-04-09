package controllers.cachefetch

import akka.Done
import cats.effect.{ContextShift, IO}
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import fetch.{Data, DataCache}
import play.api.cache.{AsyncCacheApi, NamedCache}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

import scala.concurrent.duration.FiniteDuration
import cats.syntax.applicative._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class CaffeineFetchBinder extends Module {

  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] =
    Seq(bind[CacheModule].to[CaffeineFetchModule].eagerly())
}

trait CacheModule {}

@Singleton
class CaffeineFetchModule @Inject() (
    @NamedCache("fetch-cache") fetchCache: AsyncCacheApi
)(implicit val ec: ExecutionContext)
    extends CacheModule
    with LazyLogging {

  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  logger.info("CaffeineFetchModule initialization starts")
  val cache: DataCache[IO] = CaffeineAkkaCache(fetchCache, 1.hour)
}

/**
  * Обёртка над API Play для кэша, позволяющая использовать его в Fetch
  * @param asyncAkkaCache - API кэша Play
  * @param expiration - Длительность хранения
  * @param ec - Для Future
  * @param cs - Для IO
  */
case class CaffeineAkkaCache(asyncAkkaCache: AsyncCacheApi, expiration: FiniteDuration)(
    implicit val ec: ExecutionContext,
    implicit val cs: ContextShift[IO]
) extends DataCache[IO] with LazyLogging {

  override def lookup[I, A](i: I, data: Data[I, A]): IO[Option[A]] = {
    logger.debug(s"Searching in cache $i")
    val l = asyncAkkaCache.get(i.toString)
    IO.fromFuture(IO(l))
  }

  override def insert[I, A](i: I, v: A, data: Data[I, A]): IO[DataCache[IO]] = {
    logger.debug(s"Inserting to cache $i")
    val f: Future[Done] = asyncAkkaCache.set(i.toString, v, expiration) // Результат от апи Play вернуть не получится
    this.pure[IO]
  }
}
