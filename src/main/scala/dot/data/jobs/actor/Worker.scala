package dot.data.jobs.actor

import akka.actor.{Actor, ActorRef}
import akka.event.Logging

object Worker {

  case class Start(jobActor: ActorRef)

  case object Release

}

class Worker(workerId: Int, jobManager: ActorRef) extends Actor {

  private val log = Logging(context.system, this)

  override def receive: Receive = onMessage

  private def onMessage: Receive = {

    case Worker.Release =>
      log.debug(s"Worker $workerId released")
      jobManager ! JobManager.WorkerIsReadyToTakeJob

    case Worker.Start(job) =>
      job ! Job.Start

  }
}
