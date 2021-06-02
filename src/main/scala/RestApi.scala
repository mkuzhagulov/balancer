import ServersThroughputActor._
import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class RestApi(serverThroughputActor: ActorRef)
             (implicit val actorSystem: ActorSystem, ec: ExecutionContextExecutor, t: Timeout) extends JsonSupport {


  def balanceRoute: Route =
    pathPrefix("balance") {
      pathEndOrSingleSlash {
        extractClientIP { ip =>
          val client = ip.toOption.map(_.getHostAddress).getOrElse("unknown")
          get {
            parameter("throughput".as[Int]) { num =>
              val res = (serverThroughputActor ? AllocateResources(client, num)).mapTo[Option[String]]
              onComplete(res) {
                case Success(serverOpt) =>
                  serverOpt match {
                    case Some(server) => complete(ActorResult(server))
                    case None => complete(StatusCodes.InternalServerError, NoResources("No free server available"))
                  }
                case Failure(_) => complete(StatusCodes.ServiceUnavailable)
              }
            }
          }
        }
      }
    }

  def endRoute: Route =
    pathPrefix("end") {
      pathEndOrSingleSlash {
        extractClientIP { ip =>
          val client = ip.toOption.map(_.getHostAddress).getOrElse("unknown")
          get {
            val res = (serverThroughputActor ? Finish(client)).mapTo[EndResult]
            onComplete(res) {
              case Success(result) =>
                result match {
                  case SuccessEnd => complete(StatusCodes.OK)
                  case EmptyEnd => complete(StatusCodes.NotFound, "You should request resources first")
                }
              case Failure(_) => complete(StatusCodes.ServiceUnavailable)
            }
          }
        }
      }
    }

  def routes: Route = balanceRoute ~ endRoute

}
