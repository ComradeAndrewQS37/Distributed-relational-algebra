package client

import shared.expression.Expression

// delegates computing the expression to ManagerHelper (to be sent to manager server)
suspend fun Expression.compute() = ManagerHelper.sendExpression(this)
