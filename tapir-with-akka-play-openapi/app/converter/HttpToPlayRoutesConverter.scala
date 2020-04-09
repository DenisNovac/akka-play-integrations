package converter

import akka.actor.ActorSystem
import akka.http.javadsl.server.AccessToRequestContext
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.http.scaladsl.settings.RoutingSettings
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import play.api.http.HttpEntity.{Streamed, Strict}
import play.api.libs.streams.Accumulator
import play.api.mvc.{EssentialAction, ResponseHeader, Result}
import play.api.routing.Router.Routes
import play.core.server.akkahttp.AkkaHeadersWrapper

class HttpToPlayRoutesConverter(mat: Materializer, prefix: String = "")(
    implicit system: ActorSystem
) {

  val routingSettings: RoutingSettings = RoutingSettings(system)
  import system.dispatcher

  // (c) https://github.com/johanandren/play-plus-akka-http
  def convert(r: Route): Routes = {
    case request if request.path.startsWith(prefix) =>
      EssentialAction { request =>
        // the accumulator accepts a sink that will be fed the data from the request
        val accumulator: Accumulator[ByteString, RouteResult] =
          Accumulator(Sink.asPublisher[ByteString](false).mapMaterializedValue { publisher =>
            val akkaRequest =
              request.headers.asInstanceOf[AkkaHeadersWrapper].request
            val entitySource = Source.fromPublisher(publisher)
            val requestWithBytesReintroduced = akkaRequest.withEntity(
              HttpEntity(
                akkaRequest.entity.contentType,
                akkaRequest.entity.contentLengthOption.get,
                entitySource
              )
            )

            r(
              AccessToRequestContext(
                requestWithBytesReintroduced,
                system.log,
                routingSettings
              )(system.dispatcher, mat)
            )
          })

        accumulator.map {
          case RouteResult.Complete(httpResponse) =>
            Result(
              ResponseHeader(
                httpResponse.status.intValue(),
                httpResponse.headers.map(h => h.name() -> h.value()).toMap
              ),
              Streamed(
                httpResponse.entity.dataBytes,
                httpResponse.entity.contentLengthOption,
                Some(httpResponse.entity.contentType.toString())
              )
            )
          case RouteResult.Rejected(rejections) =>
            Result(ResponseHeader(503), Strict(ByteString(), None))
        }

      }
  }

}
