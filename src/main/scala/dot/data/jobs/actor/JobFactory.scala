package dot.data.jobs.actor

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import dot.data.jobs.JobId
import dot.data.jobs.actor.JobFactory.{CreateJob, GetJob, GetStatus}

import scala.concurrent.duration._

object JobFactory {

  case class CreateJob(jobId: JobId, priority: Int)

  case class GetStatus(jobId: JobId)

  case class GetJob(jobId: JobId)

}

class JobFactory(jobQueue: ActorRef, finishedJobsQueue: ActorRef)
    extends Actor {
  private implicit val timeout: Timeout = 5.seconds
  private implicit val executionContext = Materializer(context.system).executionContext

  override def receive: Receive = {

    case GetJob(jobId) =>
      context.child(jobName(jobId)) match {
        case Some(job) => context.sender() ! Some(job)
        case None      => context.sender() ! None
      }

    case CreateJob(jobId, priority) =>
      val job = context.actorOf(
        Props(new Job(jobId, finishedJobsQueue)),
        jobName(jobId)
      )
      jobQueue ! JobManager.SubmitJob(priority, job)

    case GetStatus(jobId) =>
      context.child(jobName(jobId)) match {
        case Some(job) =>
          akka.pattern
            .pipe((job ? Job.GetStatus).map(Some(_)))
            .pipeTo(context.sender())
        case None =>
          context.sender() ! None
      }

  }

  // TODO encode JobID
  private def jobName(jobId: JobId) = "job_" + jobId

}
