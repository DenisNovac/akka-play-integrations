package controllers.datasources

import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.typesafe.scalalogging.LazyLogging
import fetch.{Data, DataSource}

class ToStringDataSource[F[_]: Concurrent] extends Data[Int, String] with LazyLogging {
  override def name: String = "Int to String"

  private def toStringInstance: Data[Int, String] = this

  val source: DataSource[F, Int, String] = new DataSource[F, Int, String] {
    override def data: Data[Int, String] = toStringInstance

    override def CF: Concurrent[F] = Concurrent[F]

    override def fetch(id: Int): F[Option[String]] =
      for {
        _ <- CF.delay(logger.info(s"[${Thread.currentThread.getId}] Calculating $id"))
        _ <- Concurrent[F].delay(Thread.sleep(100))
      } yield Option(id.toString)
  }
}
