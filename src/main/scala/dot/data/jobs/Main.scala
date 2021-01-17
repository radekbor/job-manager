package dot.data.jobs

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import dot.data.jobs.actor.{FinishedJobsQueue, JobFactory, JobManager, Worker}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.StdIn

object Main extends App {
  private implicit val system = ActorSystem("my-system")

  private val config = system.settings.config

  private val NUMBER_OF_NODES = config.getInt("app.number_of_nodes")
  private val MAX_NUMBER_OF_RETAINED_FINISHED_JOBS =
    config.getInt("app.max_number_of_retained_finished_jobs")

  println(
    s"configuration: NUMBER_OF_NODES: $NUMBER_OF_NODES, MAX_NUMBER_OF_RETAINED_FINISHED_JOBS: $MAX_NUMBER_OF_RETAINED_FINISHED_JOBS"
  )

  private implicit val timeout: Timeout = 10.seconds
  private implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  val finishedJobsQueue = system.actorOf(
    Props(new FinishedJobsQueue(MAX_NUMBER_OF_RETAINED_FINISHED_JOBS)),
    "finishedJobsQueue"
  )
  val jobManager = system.actorOf(Props(new JobManager), "jobQueue")

  val jobFactory = system.actorOf(
    Props(new JobFactory(jobManager, finishedJobsQueue)),
    "jobFactory"
  )
  val workers = Range(0, NUMBER_OF_NODES).map(
    id => system.actorOf(Props(new Worker(id, jobManager)), "worker_" + id)
  )
  val jobsActions = JobsActionsImpl(jobManager, finishedJobsQueue, jobFactory)
  val bindingFuture =
    Http().newServerAt("localhost", 8080).bind(JobsRoutes.routes(jobsActions))

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
