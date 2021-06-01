import akka.actor._
import com.typesafe.config._

object Main extends App {
  val config = ConfigFactory.load()

  val map: Map[String, Int] = Map("one" -> 1, "two" -> 1)
  println(map.updated("two", 2))

//  val system = ActorSystem("MainSystem")
}
