package dot.data.jobs

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.{Accepted, BadRequest, NotFound, OK}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dot.data.jobs.Input.{Finish, Submit}

import scala.concurrent.ExecutionContext

object JobsRoutes {

  def routes(
    jobsActions: JobsActions
  )(implicit system: ActorSystem, timeout: Timeout, ec: ExecutionContext) = {

    val submitFromBody = entity(as[Submit])
    val finishFromBody = entity(as[Finish])

    Route.seal(
      concat(
        post {
          path("submited") {
            submitFromBody { submitRequest =>
              onSuccess(jobsActions.submit(submitRequest)) {
                case true => complete(Accepted)
                case false => complete(BadRequest)
              }
            }
          }
        },
        post {
          path("finished")
          finishFromBody { finishRequest =>
            onSuccess(jobsActions.finish(finishRequest)) {
              case Left(JobNotFound) =>
                complete(NotFound)
              case Left(_) =>
                complete(BadRequest)
              case Right(_) =>
                complete(Accepted)
            }
          }
        },
        get {
          path("status" / Segment) { jobId: String =>
            onSuccess(jobsActions.status(jobId)) {
              case Some(value) =>
                complete(OK, Output.JobStatus(value.toString))
              case None =>
                complete(NotFound)
            }
          }
        },
        get {
          path("summary") {
            onSuccess(jobsActions.summary) { stats =>
              complete(OK, stats)
            }
          }
        }
      )
    )
  }
}
