import ServersThroughputActor._
import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor

class RestApi(serverThroughputActor: ActorRef)
             (implicit val actorSystem: ActorSystem, ec: ExecutionContextExecutor, t: Timeout) extends JsonSupport {


  private val ipExtractor: Directive1[String] = extractClientIP.map(_.toOption.map(_.getHostAddress).getOrElse("unknown"))

  def balanceRoute: Route =
    pathPrefix("balance") {
      pathEndOrSingleSlash {
        ipExtractor { client =>
          get {
            parameter("throughput".as[Int]) { num =>
              val res = (serverThroughputActor ? AllocateResources(client, num)).mapTo[Option[String]]
              onSuccess(res) {
                case Some(server) => complete(ActorResult(server))
                case None => complete(StatusCodes.InternalServerError, NoResources("No free server available"))
              }
            }
          }
        }
      }
    }

  def endRoute: Route =
    pathPrefix("end") {
      pathEndOrSingleSlash {
        ipExtractor { client =>
          post {
            val res = (serverThroughputActor ? Finish(client)).mapTo[EndResult]
            onSuccess(res) {
              case SuccessEnd => complete(StatusCodes.OK)
              case EmptyEnd => complete(StatusCodes.NotFound, "You should request resources first")
            }
          }
        }
      }
    }

  def routes: Route = balanceRoute ~ endRoute

}
