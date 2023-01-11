package shared

import shared.table.TableDto


/**Stores result that was sent by Worker or Manager and received by Manager or Client respectively.
 * @property [result] computed result Table in case of success and `null` otherwise
 * @property [problem] exception that was thrown during computations in case of fail and `null` otherwise**/
class ResultHolder private constructor(
    val result: TableDto?,
    val problem: Throwable?
) {

    constructor(result: TableDto) : this(result, null) // successful result
    constructor(problem: Throwable) : this(null, problem) // something went wrong

    val isSuccessful: Boolean
        get() = result != null
}
