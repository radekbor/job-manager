package dot.data.jobs

import enumeratum._

sealed trait Status extends EnumEntry

sealed trait FinialStatus extends Status

object Status extends Enum[Status] {

  override def values: IndexedSeq[Status] = findValues

  case object PENDING extends Status

  case object RUNNING extends Status

  case object SUCCEEDED extends FinialStatus

  case object FAILED extends FinialStatus

  case object UNKNOWN extends FinialStatus
}
