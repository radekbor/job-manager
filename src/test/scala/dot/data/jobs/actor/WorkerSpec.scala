package dot.data.jobs.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class WorkerSpec
    extends TestKit(ActorSystem("WorkerSpec"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with ImplicitSender {

  private val jobManager = TestProbe()
  private val job = TestProbe()

  private implicit val timeout: Timeout = 5.seconds
  private implicit val executionContext = Materializer(system).executionContext

  "Worker" should {

    "notify job when started" in {
      val worker = createWorker(1)
      jobManager.expectMsgAllOf(
        JobManager.WorkerIsReadyToTakeJob(true),
      )
      worker ! Worker.Start(job.ref)

      job.expectMsgPF() {
        case Job.Start =>
      }
    }

    "notify manager when released" in {
      val worker = createWorker(1)

      worker ! Worker.Release

      jobManager.expectMsgAllOf(
        JobManager.WorkerIsReadyToTakeJob(true),
        JobManager.WorkerIsReadyToTakeJob(false)
      )
    }

  }

  def createWorker(id: Int): ActorRef = {
    TestActorRef[Worker](Props(new Worker(id, jobManager.ref)))
  }

}
