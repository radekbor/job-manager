package dot.data.jobs

import akka.Done
import akka.actor.{ActorSystem, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.util.Timeout
import dot.data.jobs.actor.{FinishedJobsQueue, JobFactory, JobManager, Worker}
import org.log4s.{Logger, getLogger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Main {
  private implicit val system = ActorSystem("my-system")

  private val logger: Logger = getLogger
  private val config = system.settings.config
  private val port = 8080

  private implicit val timeout: Timeout = 10.seconds
  private implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  private val NUMBER_OF_NODES = config.getInt("app.number_of_nodes")
  private val MAX_NUMBER_OF_RETAINED_FINISHED_JOBS =
    config.getInt("app.max_number_of_retained_finished_jobs")

  logger.info(
    s"configuration: NUMBER_OF_NODES: $NUMBER_OF_NODES, MAX_NUMBER_OF_RETAINED_FINISHED_JOBS: $MAX_NUMBER_OF_RETAINED_FINISHED_JOBS"
  )

  def main(array: Array[String]): Unit = {
    val finishedJobsQueue = system.actorOf(
      Props(new FinishedJobsQueue(MAX_NUMBER_OF_RETAINED_FINISHED_JOBS)),
      "finishedJobsQueue"
    )
    val jobManager = system.actorOf(Props(new JobManager), "jobQueue")

    val jobFactory = system.actorOf(
      Props(new JobFactory(jobManager, finishedJobsQueue)),
      "jobFactory"
    )
    Range(0, NUMBER_OF_NODES).foreach(
      id => {
        system.actorOf(Props(new Worker(id, jobManager)), "worker_" + id)
      }
    )
    val jobsActions = JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)

    val serverBinding =
      Http().newServerAt("0.0.0.0", port).bind(JobsRoutes.routes(jobsActions))

    serverBinding.onComplete {
      case Success(binding) =>
        logger.info(s"Server started on $port")
        sys.addShutdownHook(shutdownBinding(binding))

      case Failure(e) =>
        logger.error(s"Failed to start server on $port - ${e.getMessage}")
        shutdownSystem()
    }
  }



  private val shutdownSystem: () => Future[Terminated] =
    () =>
      system
        .terminate()
        .andThen { case attempt => logger.info(s"Attempted to stop ActorSystem - $attempt") }

  private val shutdownBinding: ServerBinding => Future[Done] = _.unbind()
    .andThen {
      case Success(_) =>
        logger.info("Server stopped")
        shutdownSystem()

      case Failure(e) =>
        logger.error(s"Failed to stop the server - ${e.getMessage}")
    }



}
