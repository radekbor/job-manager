package dot.data.jobs

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import dot.data.jobs.Input.Finish
import dot.data.jobs.Status.SUCCEEDED
import dot.data.jobs.actor.{FinishedJobsQueue, Job, JobFactory, JobManager}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class JobActionsImplSpec
    extends TestKit(ActorSystem("JobActionsImplSpec"))
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with ImplicitSender {

  private implicit val timeout: Timeout = 5.seconds
  private implicit val executionContext = Materializer(system).executionContext

  private val jobManager: ActorRef = system.actorOf(Props.empty)
  private val finishedJobsQueue: ActorRef = system.actorOf(Props.empty)

  "JobActionsImpl" should {

    "send create job message to job when submit invoked" in {
      val jobFactoryProbe = TestProbe()
      val jobFactory: ActorRef = jobFactoryProbe.ref
      val jobActions =
        JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

      jobActions.submit(Input.Submit("1", 1))

      jobFactoryProbe.expectMsgPF() {
        case JobFactory.CreateJob(_, 1) => 1
      }

    }

    "get status when job factory returns status" in {
      val jobFactory = system.actorOf(Props(new Actor {
        override def receive: Receive = {
          case JobFactory.GetStatus(_) => sender() ! Some(SUCCEEDED)
        }
      }))
      val jobActions =
        JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

      jobActions.status("1").futureValue should be(Some(SUCCEEDED))
    }

    "get not when job factory returns none" in {
      val jobFactory = system.actorOf(Props(new Actor {
        override def receive: Receive = {
          case JobFactory.GetStatus(_) => sender() ! None
        }
      }))
      val jobActions =
        JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

      jobActions.status("1").futureValue should be(None)
    }

    "send finish message to job when finish invoked" in {
      val job = TestProbe()
      val jobFactoryProbe = system.actorOf(Props(new Actor() {
        override def receive: Receive = {
          case JobFactory.GetJob(_) => sender() ! Some(job.ref)
        }
      }))
      val jobFactory: ActorRef = jobFactoryProbe
      val jobActions =
        JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

      jobActions
        .finish(Finish("1", "SUCCEEDED"))
        .futureValue should matchPattern {
        case Right(_) =>
      }

      job.expectMsgPF() {
        case Job.Finish(SUCCEEDED) =>
      }

    }

    "return error when job doesn't exist" in {
      val jobFactoryProbe = system.actorOf(Props(new Actor() {
        override def receive: Receive = {
          case JobFactory.GetJob(_) => sender() ! None
        }
      }))
      val jobFactory: ActorRef = jobFactoryProbe
      val jobActions =
        JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

      jobActions
        .finish(Finish("1", "SUCCEEDED"))
        .futureValue should matchPattern {
        case Left(JobNotFound) =>
      }
    }

    "return error and send no message to job when incorrect state name" in {
      val job = TestProbe()
      val jobFactoryProbe = system.actorOf(Props(new Actor() {
        override def receive: Receive = {
          case JobFactory.GetJob(_) => sender() ! Some(job.ref)
        }
      }))
      val jobFactory: ActorRef = jobFactoryProbe
      val jobActions =
        JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

      jobActions
        .finish(Finish("1", "sss"))
        .futureValue should matchPattern {
        case Left(IncorrectStateName) =>
      }

      job.expectNoMessage()

    }

    "return error and send no message to job when not final state name" in {
      val job = TestProbe()
      val jobFactoryProbe = system.actorOf(Props(new Actor() {
        override def receive: Receive = {
          case JobFactory.GetJob(_) => sender() ! Some(job.ref)
        }
      }))
      val jobFactory: ActorRef = jobFactoryProbe
      val jobActions =
        JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

      jobActions
        .finish(Finish("1", "RUNNING"))
        .futureValue should matchPattern {
        case Left(NotFinalStatus) =>
      }

      job.expectNoMessage()

    }

    "combine statistics from job manager and finished job queue when ask for summary" in {
      val jobManager = system.actorOf(Props(new Actor() {
        override def receive: Receive = {
          case JobManager.CountPendingAndRunning =>
            sender() ! PendingAndRunningCount(10, 20)
        }
      }))

      val finishedJobsQueue = system.actorOf(Props(new Actor() {
        override def receive: Receive = {
          case FinishedJobsQueue.GetStats =>
            sender() ! FinishedJobStats(30, 40)
        }
      }))

      val jobFactory: ActorRef = system.actorOf(Props.empty)
      val jobActions =
        JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

      jobActions.summary.futureValue should matchPattern {
        case Output.JobStats(10, 20, 30, 40) =>
      }
    }

  }

}
