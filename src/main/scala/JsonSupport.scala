import spray.json._

trait JsonSupport extends DefaultJsonProtocol {
  case class ActorResult(server: String)
  case class NoResources(err: String)

  implicit val resultFormat: RootJsonFormat[ActorResult] = jsonFormat1(ActorResult)
  implicit val noResourcesFormat: RootJsonFormat[NoResources] = jsonFormat1(NoResources)
}


