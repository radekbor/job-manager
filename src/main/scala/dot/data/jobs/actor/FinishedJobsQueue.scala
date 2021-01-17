package dot.data.jobs.actor

import akka.actor.Actor
import akka.event.Logging
import dot.data.jobs.{FinialStatus, FinishedJob, FinishedJobStats, JobId}

import scala.collection.immutable.Queue

object FinishedJobsQueue {

  case class Finished(jobId: JobId, finishState: FinialStatus)

  case object GetStats

}

// We could verify if maxSize > 0
class FinishedJobsQueue(maxSize: Int) extends Actor {

  private val log = Logging(context.system, this)

  override def receive: Receive = onMessage(Queue.empty)

  private def onMessage(queue: Queue[FinishedJob]): Receive = {

    case FinishedJobsQueue.Finished(jobId, state) =>
      val withFinishedJob = queue.appended(FinishedJob(jobId, state))
      val finalQueue = if (withFinishedJob.size > maxSize) {
        log.info("exceeded max num of finished jobs")
        withFinishedJob.tail
      } else {
        withFinishedJob
      }
      context.become(onMessage(finalQueue))

    case FinishedJobsQueue.GetStats =>
      val stats = queue.groupMapReduce(_.state)(_ => 1L)(_ + _)
      context.sender() ! FinishedJobStats(stats)
  }
}
