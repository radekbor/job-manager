package dot.data.jobs.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import dot.data.jobs.Status.{FAILED, SUCCEEDED}
import dot.data.jobs.{FinishedJobStats, JobId}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class FinishedJobsQueueSpec
    extends TestKit(ActorSystem("FinishedJobsQueueSpec"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with ImplicitSender {

  private implicit val timeout: Timeout = 5.seconds
  private implicit val executionContext = Materializer(system).executionContext

  "FinishedJobsQueue" should {

    "remove the oldest message when exceeded capacity and count correctly" in {
      val queue = createQueue(3)

      queue ! FinishedJobsQueue.Finished(JobId("1"), SUCCEEDED)
      queue ! FinishedJobsQueue.Finished(JobId("2"), FAILED)
      queue ! FinishedJobsQueue.Finished(JobId("3"), FAILED)
      queue ! FinishedJobsQueue.Finished(JobId("4"), SUCCEEDED)

      (queue ? FinishedJobsQueue.GetStats)
        .mapTo[FinishedJobStats]
        .futureValue should matchPattern {
        case FinishedJobStats(1, 2) =>
      }
    }

  }

  def createQueue(size: Int): ActorRef = {
    TestActorRef[FinishedJobsQueue](Props(new FinishedJobsQueue(size)))
  }

}
