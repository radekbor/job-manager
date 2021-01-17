package dot.data.jobs.actor

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import dot.data.jobs.{FinialStatus, JobId, Status}

object Job {

  case class Finish(state: FinialStatus)

  case object GetStatus

  case object Start

  case object Destroy

}

class Job(jobId: JobId, finishedJobsQueue: ActorRef) extends Actor {

  private val log = Logging(context.system, this)

  override def receive: Receive = onMessage(Status.PENDING, None)

  private def onMessage(status: Status,
                        maybeWorker: Option[ActorRef]): Receive = {

    case Job.GetStatus =>
      sender() ! status

    case Job.Start =>
      context.become(onMessage(Status.RUNNING, Some(sender())))

    case Job.Finish(status) =>
      maybeWorker match {
        case Some(worker) =>
          worker ! Worker.Release
          finishedJobsQueue ! FinishedJobsQueue.Finished(jobId, status)
          context.become(onMessage(status, None))
        case None =>
          log.info("incorrect message, job has no worker")
      }

    case Job.Destroy =>
      context.stop(self)
  }

}
