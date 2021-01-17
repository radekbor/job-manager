package dot.data.jobs

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

object Input {

  implicit def configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  case class Submit(jobId: String, priority: Int)

  case class Finish(jobId: String, status: String)

  object Submit {
    implicit val decoder: Decoder[Submit] = deriveConfiguredDecoder
  }

  object Finish {
    implicit val decoder: Decoder[Finish] = deriveConfiguredDecoder
  }

}
