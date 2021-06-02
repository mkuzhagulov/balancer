import ServersThroughputActor._
import akka.actor._
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import com.typesafe.config.{Config, ConfigFactory}

class ServerThroughputActorSpec extends TestKit(ActorSystem("TestSystem"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val testConfigString: String =
    """
      | servers = [
      |   "127.0.0.1 50",
      |   "127.0.0.2 100",
      |   "127.0.0.3 1"
      | ]
      |""".stripMargin

  val testConfig: Config = ConfigFactory.parseString(testConfigString)
  val client = "10.11.12.13"
  val otherClient = "22.33.44.55"

  "ServerThroughputActor" should {
    "allocate server with max amount of resources" in {
      val testActor = system.actorOf(Props(new ServersThroughputActor(testConfig)))
      testActor ! Init
      expectNoMessage()

      testActor ! AllocateResources(client, 30)
      expectMsg(Some("127.0.0.2"))
    }

    "allocate and release resources" in {
      val testActor = system.actorOf(Props(new ServersThroughputActor(testConfig)))
      testActor ! Init
      expectNoMessage()

      testActor ! AllocateResources("10.11.12.13", 100)
      expectMsg(Some("127.0.0.2"))

      testActor ! Finish(client)
      expectMsg(SuccessEnd)

      testActor ! AllocateResources("10.11.12.13", 100)
      expectMsg(Some("127.0.0.2"))
    }

    "return failure if service has not enough resources" in {
      val testActor = system.actorOf(Props(new ServersThroughputActor(testConfig)))
      testActor ! Init
      expectNoMessage()

      testActor ! AllocateResources("10.11.12.13", 300)
      expectMsg(None)
    }

    "not allow to client release other resources" in {
      val testActor = system.actorOf(Props(new ServersThroughputActor(testConfig)))
      testActor ! Init
      expectNoMessage()

      testActor ! AllocateResources("10.11.12.13", 5)
      expectMsg(Some("127.0.0.2"))

      testActor ! Finish(otherClient)
      expectMsg(EmptyEnd)
    }
  }
}
