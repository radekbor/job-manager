package dot.data.jobs.actor

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import dot.data.jobs.{JobId, Status}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class JobFactorySpec
    extends TestKit(ActorSystem("JobFactorySpec"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures {

  private val jobManager = TestProbe()
  private val finishedJobsQueue = TestProbe()
  private val jobFactory =
    TestActorRef[JobFactory](
      Props(new JobFactory(jobManager.ref, finishedJobsQueue.ref))
    )

  private implicit val timeout: Timeout = 5.seconds
  private implicit val executionContext = Materializer(system).executionContext

  "JobFactory" should {

    "be able to create new job and send to queue with pending state" in {

      jobFactory ! JobFactory.CreateJob(JobId("1"), 1)

      jobManager.expectMsgPF() {
        case JobManager.SubmitJob(1, _) =>
      }

      finishedJobsQueue.expectNoMessage()

      val res = (jobFactory ? JobFactory.GetStatus(JobId("1")))
        .mapTo[Option[Status]]
        .futureValue

      res should matchPattern {
        case Some(Status.PENDING) =>
      }
    }

    "return none when job doesn't exist" in {

      val res = (jobFactory ? JobFactory.GetStatus(JobId("XYZ")))
        .mapTo[Option[Status]]
        .futureValue

      res should matchPattern {
        case None =>
      }
    }

  }

}
