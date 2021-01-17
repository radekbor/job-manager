package dot.data

import dot.data.jobs.Status.{FAILED, SUCCEEDED}

package object jobs {

  final case class JobId(underlying: String)
      extends AnyVal
      with Comparable[JobId] {

    override def toString: String = underlying

    override def compareTo(o: JobId): Int = underlying.compareTo(o.underlying)
  }

  case class FinishedJob(jobId: JobId, state: FinialStatus)

  case class FinishedJobStats(succeeded: Long, failed: Long)

  case class PendingAndRunningCount(count: Long, waiting: Long)

  case class JobStatus(maybeState: Option[Status])

  object FinishedJobStats {
    def apply(map: Map[FinialStatus, Long]): FinishedJobStats =
      new FinishedJobStats(
        map.getOrElse(SUCCEEDED, 0L),
        map.getOrElse(FAILED, 0L)
      )
  }
}
