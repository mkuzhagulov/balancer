import ServersThroughputActor.Init
import akka.actor._
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success}

object Main extends App {
  val config = ConfigFactory.load()

  implicit val system: ActorSystem = ActorSystem("mainSystem")
  implicit val timeout: Timeout = requestToTimeout(config)
  import system.dispatcher

  val log = Logging(system.eventStream, "Balancer")

  val serverThroughputActor = system.actorOf(Props(new ServersThroughputActor(config)))
  serverThroughputActor ! Init

  val restApi = new RestApi(serverThroughputActor)

  val futureBinding = Http().newServerAt("0.0.0.0", 8080).bind(restApi.routes)

  futureBinding onComplete {
    case Success(binding) =>
      log.info("Balancer bound to {}", binding.localAddress)
    case Failure(ex) =>
      log.error(ex, "Failed to bind to localhost")
  }

  def requestToTimeout(conf: Config): Timeout = {
    val t = config.getString("akka.actor.timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
