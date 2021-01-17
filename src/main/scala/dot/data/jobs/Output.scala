package dot.data.jobs

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object Output {

  case class JobStats(pending: Long,
                      running: Long,
                      succeeded: Long,
                      failed: Long)

  case class JobStatus(status: String)

  object JobStats {
    def apply(pending: Long,
              running: Long,
              finishedJobStats: FinishedJobStats): JobStats = new JobStats(
      pending,
      running,
      finishedJobStats.succeeded,
      finishedJobStats.failed
    )

    implicit val encoder: Encoder[JobStats] = deriveEncoder
    implicit val decoder: Decoder[JobStats] = deriveDecoder
  }

  object JobStatus {
    implicit val encoder: Encoder[JobStatus] = deriveEncoder
    implicit val decoder: Decoder[JobStatus] = deriveDecoder
  }
}
