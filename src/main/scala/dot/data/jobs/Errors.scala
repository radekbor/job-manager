package dot.data.jobs

sealed trait FinalActionError
case object JobNotFound extends FinalActionError
case object IncorrectStateName extends FinalActionError
case object NotFinalStatus extends FinalActionError
