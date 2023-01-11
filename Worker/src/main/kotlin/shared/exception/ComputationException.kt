package shared.exception

/**Thrown when worker returned exceptional result for one of tasks**/
class ComputationException(message: String?, cause: Throwable?) : Exception(message, cause) {
}