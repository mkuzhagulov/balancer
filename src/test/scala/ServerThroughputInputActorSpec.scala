import ServersThroughputActor._
import akka.actor._
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import com.typesafe.config.{Config, ConfigFactory}

class ServerThroughputInputActorSpec extends TestKit(ActorSystem("TestSystem"))
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

  "ServerThroughputInputActor" should {
    "allocate server with max amount of resources" in {
      val testActor = system.actorOf(Props(new ServersThroughputActor(testConfig)))
      testActor ! Init
      expectNoMessage()

      testActor ! AllocateResources("10.11.12.13", 30)
      val reply = expectMsgType[AllocatedServer]
      reply.server mustBe "127.0.0.2"
    }

    "allocate and release resources" in {
      val testActor = system.actorOf(Props(new ServersThroughputActor(testConfig)))
      testActor ! Init
      expectNoMessage()

      testActor ! AllocateResources("10.11.12.13", 100)
      val reply1 = expectMsgType[AllocatedServer]
      reply1.server mustBe "127.0.0.2"

      testActor ! Finish("10.11.12.13")
      expectNoMessage()

      testActor ! AllocateResources("10.11.12.13", 100)
      val reply2 = expectMsgType[AllocatedServer]
      reply2.server mustBe "127.0.0.2"
    }

    "return failure if service have not enough resources" in {
      val testActor = system.actorOf(Props(new ServersThroughputActor(testConfig)))
      testActor ! Init
      expectNoMessage()

      testActor ! AllocateResources("10.11.12.14", 300)
      expectMsg(NoResources)
    }
  }
}
