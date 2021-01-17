package dot.data.jobs

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.util.Timeout
import dot.data.jobs.Output.JobStats
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.{JsNumber, JsString, JsValue}

import scala.concurrent.Future
import scala.concurrent.duration._

class JobsRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  private implicit val timeout: Timeout = 10.seconds

  "POST /submited" should {

    "be able parse correct input and return Accepted" in {

      val jobsActions: JobsActions = new JobsActionsMock()

      val routes = JobsRoutes.routes(jobsActions)

      val entity =
        HttpEntity(
          ContentTypes.`application/json`,
          """{"job_id": "String", "priority": 1}"""
        )
      Post("/submited", entity) ~> routes ~> check {
        status should ===(StatusCodes.Accepted)
      }

    }

    "return an error when submit request is incorrect" in {

      val jobsActions: JobsActions = new JobsActionsMock()

      val routes = JobsRoutes.routes(jobsActions)

      val entity =
        HttpEntity(ContentTypes.`application/json`, """{"job_id": "String"}""")
      Post("/submited", entity) ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
      }

    }
  }

  "POST /finish" should {

    "be able parse correct input and return Accepted" in {
      val jobsActions: JobsActions = new JobsActionsMock()

      val routes = JobsRoutes.routes(jobsActions)

      val entity =
        HttpEntity(
          ContentTypes.`application/json`,
          """{"job_id": "String", "status": "SUCCEEDED"}"""
        )
      Post("/finished", entity) ~> routes ~> check {
        status should ===(StatusCodes.Accepted)
      }
    }

    "return and error when job not found " in {

      val jobsActions: JobsActions = new JobsActionsMock() {
        override def finish(
          finish: Input.Finish
        ): Future[Either[FinalActionError, Unit]] = {
          Future.successful(Left(JobNotFound))
        }
      }

      val routes = JobsRoutes.routes(jobsActions)

      val entity =
        HttpEntity(
          ContentTypes.`application/json`,
          """{"job_id": "String", "status": "SUCCEEDED"}"""
        )
      Post("/finished", entity) ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }

    }

    "return and error when invalid state provided " in {

      val jobsActions: JobsActions = new JobsActionsMock() {
        override def finish(
                             finish: Input.Finish
                           ): Future[Either[FinalActionError, Unit]] = {
          Future.successful(Left(IncorrectStateName))
        }
      }

      val routes = JobsRoutes.routes(jobsActions)

      val entity =
        HttpEntity(
          ContentTypes.`application/json`,
          """{"job_id": "String", "status": "XYZ"}"""
        )
      Post("/finished", entity) ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
      }

    }

  }

  "GET /status/{jobId}" should {

    "return response in correct format" in {
      val jobsActions: JobsActions = new JobsActionsMock()

      val routes = JobsRoutes.routes(jobsActions)

      Get("/status/abc") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val map = responseAs[JsValue].asJsObject.fields

        map("status") should be (JsString("SUCCEEDED"))
      }
    }

    "return and error when job doesn't exist" in {
      val jobsActions: JobsActions = new JobsActionsMock() {
        override def status(jobId: String): Future[Option[Status]] = Future.successful(None)
      }

      val routes = JobsRoutes.routes(jobsActions)

      Get("/status/abc") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }

  }

  "GET /summary" should {

    "return response in correct format" in {

      val jobsActions: JobsActions = new JobsActionsMock()

      val routes = JobsRoutes.routes(jobsActions)

      Get("/summary") ~> routes ~> check {
        status should ===(StatusCodes.OK)

        val map = responseAs[JsValue].asJsObject.fields

        map("pending") should be (JsNumber(1))
        map("running") should be (JsNumber(2))
        map("succeeded") should be (JsNumber(3))
        map("failed") should be (JsNumber(4))
      }

    }

  }

  class JobsActionsMock extends JobsActions {
    override def submit(submit: Input.Submit): Future[Boolean] = Future.successful(true)

    override def finish(
      finish: Input.Finish
    ): Future[Either[FinalActionError, Unit]] =
      Future.successful(Right({}))

    override def status(jobId: String): Future[Option[Status]] =
      Future.successful(Some(Status.SUCCEEDED))

    override def summary: Future[Output.JobStats] =
      Future.successful(JobStats(1, 2, 3, 4))
  }
}
