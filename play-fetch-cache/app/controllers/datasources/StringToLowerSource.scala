package controllers.datasources

import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.typesafe.scalalogging.LazyLogging
import fetch.{Data, DataSource}

class StringToLowerSource[F[_]: Concurrent] extends Data[String, String] with LazyLogging {
  override def name: String = "String to lower case"

  private def inst: Data[String, String] = this

  val source: DataSource[F, String, String] = new DataSource[F, String, String] {
    override def data: Data[String, String] = inst

    override def CF: Concurrent[F] = Concurrent[F]

    override def fetch(id: String): F[Option[String]] =
      for {
        _ <- CF.delay(logger.debug(s"[${Thread.currentThread.getId}] Lowering $id"))
        _ <- Concurrent[F].delay(Thread.sleep(100))
      } yield Option(id.toLowerCase)
  }
}
