package dot.data.jobs.actor

import akka.actor.{Actor, ActorRef}
import org.log4s.{Logger, getLogger}

object Worker {

  case class Start(jobActor: ActorRef)

  case object Release

}

class Worker(workerId: Int, jobManager: ActorRef) extends Actor {

  jobManager ! JobManager.WorkerIsReadyToTakeJob(true)

  private val logger: Logger = getLogger

  override def receive: Receive = onMessage

  private def onMessage: Receive = {

    case Worker.Release =>
      logger.info(s"Worker $workerId released")
      jobManager ! JobManager.WorkerIsReadyToTakeJob(false)

    case Worker.Start(job) =>
      job ! Job.Start

  }
}
