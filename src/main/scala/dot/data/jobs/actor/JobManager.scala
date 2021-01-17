package dot.data.jobs.actor

import akka.actor.{Actor, ActorRef}
import dot.data.jobs.PendingAndRunningCount
import dot.data.jobs.actor.JobManager.CountPendingAndRunning

import scala.collection.immutable.TreeSet

object JobManager {

  case class SubmitJob(priority: Int, jobActor: ActorRef)

  case object WorkerIsReadyToTakeJob

  case object CountPendingAndRunning

}

class JobManager extends Actor {

  override def receive: Receive =
    onMessage(new TreeSet[JobManager.SubmitJob]()(ordering), 0, List.empty)

  private implicit val ordering: Ordering[JobManager.SubmitJob] =
    new Ordering[JobManager.SubmitJob] {
      override def compare(x: JobManager.SubmitJob,
                           y: JobManager.SubmitJob): Int =
        x.priority.compareTo(y.priority) match {
          case 0 => x.jobActor.compareTo(y.jobActor)
          case x => x
        }
    }.reverse

  private def onMessage(queue: TreeSet[JobManager.SubmitJob],
                        working: Long,
                        waitingWorkers: List[ActorRef]): Receive = {

    case submitJob: JobManager.SubmitJob =>
      waitingWorkers match {
        case head :: tail =>
          head ! Worker.Start(submitJob.jobActor)
          context.become(onMessage(queue, working + 1, tail))
        case Nil =>
          context.become(onMessage(queue + submitJob, working, Nil))
      }

    case JobManager.WorkerIsReadyToTakeJob =>
      queue.headOption match {
        case Some(job) =>
          context.sender() ! Worker.Start(job.jobActor)
          context.become(onMessage(queue.tail, working - 1, waitingWorkers))
        case None =>
          context.become(
            onMessage(queue, working, context.sender() :: waitingWorkers)
          )
      }

    case CountPendingAndRunning =>
      context.sender() ! PendingAndRunningCount(queue.size, working)

  }
}
