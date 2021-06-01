import akka.actor._
import com.typesafe.config.Config

import scala.collection.JavaConverters._

class ServersThroughputActor(config: Config) extends Actor with ActorLogging {
  import ServersThroughputActor._

  val serversList: List[String] = config.getStringList("servers").asScala.toList
  val serversStateMap: Map[String, Int] = serversList.map(x => x.split(" ")).map {
    case Array(k, v) => k -> v.toInt
  }.toMap

  val clientsMap = Map.empty[String, ServerResources]

  def receive: Receive = {
    case Init => context become updateStatesReceive(serversStateMap, clientsMap)
  }

  def updateStatesReceive(servers: Map[String, Int], clients: Map[String, ServerResources]): Receive = {
    case AllocateResources(client, amount) if !clients.contains(client) => findResources(amount, servers) match {
      case Some(server) =>
        val newServersState = servers.updated(server, servers(server) - amount)
        val newClientsState = clients.updated(client, ServerResources(server, amount))

        context become updateStatesReceive(newServersState, newClientsState)
        sender() ! AllocatedServer(server)
        log.info("{} resources allocated for client: '{}'", amount, client)

      case None => sender() ! NoResources
    }

    // Сервис позволяет одному клиенту запросить ресурсы один раз до освобождения им ресурсов
    case AllocateResources(_, _) => sender() ! NoResources
    case Finish(client) =>
      clients.get(client) match {
        case Some(serverResources) =>
          val newServersState = servers.updated(serverResources.server, servers(serverResources.server)
            + serverResources.resources)
          val newClientsState = clients - client

          context become updateStatesReceive(newServersState, newClientsState)
          log.info("Client: '{}' released resources", client)

        case None => log.warning("Client: '{}' is trying to release empty resources", client)
      }
  }
}

object ServersThroughputActor {
  case class ServerResources(server: String, resources: Int)

  case object Init
  case class AllocateResources(client: String, num: Int)
  case class AllocatedServer(server: String)
  case object NoResources
  case class Finish(client: String)

  def findResources(throughput: Int, serversState: Map[String, Int]): Option[String] = {
    serversState.reduceLeft((x, y) => if (x._2 >= y._2) x else y) match {
      case (server, resources) if resources >= throughput => Some(server)
      case _ => None
    }
  }
}

