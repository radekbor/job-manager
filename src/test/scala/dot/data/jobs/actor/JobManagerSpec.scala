package dot.data.jobs.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import dot.data.jobs.PendingAndRunningCount
import dot.data.jobs.actor.JobManager.CountPendingAndRunning
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class JobManagerSpec
    extends TestKit(ActorSystem("JobSpec"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with ImplicitSender {

  private val workerProbe1 = TestProbe()
  private val workerProbe2 = TestProbe()

  private implicit val timeout: Timeout = 5.seconds
  private implicit val executionContext = Materializer(system).executionContext

  "JobManager" should {

    "not reply when there are no jobs" in {
      val jobManager = createJobManager

      jobManager.tell(JobManager.WorkerIsReadyToTakeJob, workerProbe1.ref)

      workerProbe1.expectNoMessage()

    }

    "reply to worker when there is a job to be taken with highest priority and remove from queue" in {
      val job1 = system.actorOf(Props.empty)
      val job2 = system.actorOf(Props.empty)
      val job3 = system.actorOf(Props.empty)
      val lastJob = system.actorOf(Props.empty)

      val jobManager = createJobManager
      jobManager ! JobManager.SubmitJob(1, job1)
      jobManager ! JobManager.SubmitJob(3, job3)
      jobManager ! JobManager.SubmitJob(2, job2)

      val res1 = (jobManager ? JobManager.WorkerIsReadyToTakeJob)
        .mapTo[Worker.Start]
        .futureValue

      res1 should be(Worker.Start(job3))

      val res2 = (jobManager ? JobManager.WorkerIsReadyToTakeJob)
        .mapTo[Worker.Start]
        .futureValue

      res2 should be(Worker.Start(job2))

    }

    "send message to worker when there are waiting workers" in {
      val jobManager = createJobManager
      val job1 = system.actorOf(Props.empty)
      val job2 = system.actorOf(Props.empty)

      jobManager.tell(JobManager.WorkerIsReadyToTakeJob, workerProbe1.ref)
      jobManager.tell(JobManager.WorkerIsReadyToTakeJob, workerProbe2.ref)

      (jobManager ? CountPendingAndRunning)
        .mapTo[PendingAndRunningCount]
        .futureValue should matchPattern {
        case PendingAndRunningCount(0, 0) =>
      }

      jobManager ! JobManager.SubmitJob(1, job1)
      (jobManager ? CountPendingAndRunning)
        .mapTo[PendingAndRunningCount]
        .futureValue should matchPattern {
        case PendingAndRunningCount(0, 1) =>
      }
      jobManager ! JobManager.SubmitJob(2, job2)
      (jobManager ? CountPendingAndRunning)
        .mapTo[PendingAndRunningCount]
        .futureValue should matchPattern {
        case PendingAndRunningCount(0, 2) =>
      }

      workerProbe2.expectMsgPF() {
        case Worker.Start(`job1`) =>
      }

      workerProbe1.expectMsgPF() {
        case Worker.Start(`job2`) =>
      }

    }

    "handle two jobs with same priority" in {
      val jobManager = createJobManager

      val job1 = system.actorOf(Props.empty)
      val job2 = system.actorOf(Props.empty)

      jobManager.tell(JobManager.WorkerIsReadyToTakeJob, workerProbe1.ref)
      jobManager.tell(JobManager.WorkerIsReadyToTakeJob, workerProbe2.ref)

      jobManager ! JobManager.SubmitJob(1, job1)
      jobManager ! JobManager.SubmitJob(1, job2)

      val invokedJobAt1 = workerProbe1.fishForSpecificMessage() {
        case Worker.Start(j) => j
      }
      val invokedJobAt2 = workerProbe2.fishForSpecificMessage() {
        case Worker.Start(j) => j
      }

      invokedJobAt1 should not be invokedJobAt2

    }
  }

  def createJobManager: ActorRef = {
    TestActorRef[JobManager](Props(new JobManager))
  }

}
