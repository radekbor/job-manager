package dot.data.jobs

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import dot.data.jobs.Input.Finish
import dot.data.jobs.actor.JobFactory.CreateJob
import dot.data.jobs.actor.{FinishedJobsQueue, Job, JobFactory, JobManager}

import scala.concurrent.{ExecutionContext, Future}

trait JobsActions {
  def submit(submit: Input.Submit): Future[Boolean]

  def finish(finish: Finish): Future[Either[FinalActionError, Unit]]

  def status(jobId: String): Future[Option[Status]]

  def summary: Future[Output.JobStats]
}
case class JobsActionsImpl(
  jobManager: ActorRef,
  finishedJobsQueue: ActorRef,
  jobFactory: ActorRef
)(implicit ec: ExecutionContext, timeout: Timeout)
    extends JobsActions {

  def submit(submit: Input.Submit): Future[Boolean] =
    (jobFactory ? CreateJob(JobId(submit.jobId), submit.priority))
      .mapTo[Boolean]

  def finish(finish: Finish): Future[Either[FinalActionError, Unit]] = {
    val jobId = JobId(finish.jobId)
    val maybeStatus = {
      Status.withNameInsensitiveOption(finish.status) match {
        case Some(state: FinialStatus) =>
          Right(state)
        case Some(_) =>
          Left(NotFinalStatus)
        case _ =>
          Left(IncorrectStateName)
      }
    }
    for {
      maybeJob <- (jobFactory ? JobFactory.GetJob(jobId))
        .mapTo[Option[ActorRef]]
      res <- (maybeStatus, maybeJob) match {
        case (Left(error), _) =>
          Future {
            Left(error)
          }
        case (_, None) =>
          Future {
            Left(JobNotFound)
          }
        case (Right(status), Some(job)) =>
          Future { Right(job ! Job.Finish(status)) }

      }
    } yield res
  }

  override def status(jobId: String): Future[Option[Status]] = {
    val id = JobId(jobId)
    (jobFactory ? JobFactory.GetStatus(id)).mapTo[Option[Status]]
  }

  override def summary: Future[Output.JobStats] =
    for {
      PendingAndRunningCount(pending, running) <- (jobManager ? JobManager.CountPendingAndRunning)
        .mapTo[PendingAndRunningCount]
      finishedJobStats <- (finishedJobsQueue ? FinishedJobsQueue.GetStats)
        .mapTo[FinishedJobStats]
    } yield Output.JobStats(pending, running, finishedJobStats)
}
