package shared

import shared.utils.Operation
import shared.table.TableDto
import kotlinx.coroutines.CompletableDeferred


/**Represents one operation on tables.
[TaskHolder] objects are serialised and sent to Workers**/
class TaskHolder(
    val operation: Operation,
    /* if this task is dependent, i.e. other task needs to be executed first,
    one arg (or both) will be null, but corresponding future objects will be available
     */
    var arg1: TableDto?,
    var arg2: TableDto?,
    @Transient val arg1Future: CompletableDeferred<TableDto>? = null,
    @Transient val arg2Future: CompletableDeferred<TableDto>? = null,
    // where to send the result of task
    @Transient val resultTo: CompletableDeferred<TableDto>? = null
)


