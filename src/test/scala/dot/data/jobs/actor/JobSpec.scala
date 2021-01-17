package dot.data.jobs.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{AskTimeoutException, ask}
import akka.stream.Materializer
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import dot.data.jobs.{JobId, Status}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class JobSpec
    extends TestKit(ActorSystem("JobSpec"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with ImplicitSender {

  private val finishedJobsQueue = TestProbe()

  private implicit val timeout: Timeout = 5.seconds
  private implicit val executionContext = Materializer(system).executionContext

  "Job" should {

    "be able to tell about status when just started" in {
      val job = createJob("1")

      val res = (job ? Job.GetStatus).mapTo[Status].futureValue

      res should be(Status.PENDING)
    }

    "be able to tell about status when finished" in {
      val job = createJob("2")
      job ! Job.Start

      val res = (job ? Job.GetStatus).mapTo[Status].futureValue

      res should be(Status.RUNNING)
    }

    "be able to handle Finish" in {
      val job = createJob("2")
      job ! Job.Start

      job ! Job.Finish(Status.SUCCEEDED)

      finishedJobsQueue.expectMsgPF() {
        case FinishedJobsQueue.Finished(JobId("2"), Status.SUCCEEDED) =>
      }

      val res = (job ? Job.GetStatus).mapTo[Status].futureValue
      res should be(Status.SUCCEEDED)

    }

    "be able to handle destroy" in {
      val job = createJob("2")
      job ! Job.Destroy

      val f = job ? Job.GetStatus

      ScalaFutures.whenReady(f.failed) { e =>
        e shouldBe a[AskTimeoutException]
      }
    }

  }

  def createJob(id: String): ActorRef = {
    TestActorRef[Job](Props(new Job(JobId(id), finishedJobsQueue.ref)))
  }

}
