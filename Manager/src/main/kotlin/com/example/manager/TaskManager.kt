package com.example.manager

import shared.TaskHolder
import shared.expression.ExpressionDto
import shared.expression.ExpressionHolder
import shared.table.TableDto
import shared.utils.Logger
import kotlinx.coroutines.*

object TaskManager {

    // send all tasks and wait for final result
    suspend fun processExpressionAsync(expr: ExpressionHolder): Deferred<TableDto> {
        // will store final result
        val deferredResult = CompletableDeferred<TableDto>()
        val tasksList = expr.toTaskList(resTo = deferredResult)


        Logger.log("started sending tasks to workers")
        withContext(Dispatchers.Default) {
            tasksList.map { task ->
                launch {
                    RabbitmqManager.sendTask(task)
                }
            }
        }


        return deferredResult
    }


    // traverse the "tree" of expression and linearize it
    private fun ExpressionHolder.toTaskList(resTo: CompletableDeferred<TableDto>? = null): List<TaskHolder> {
        val tasksList: MutableList<TaskHolder> = mutableListOf()
        traverseExprRec(this@toTaskList.rootExpr, tasksList, resTo, this@toTaskList.argsList)

        return tasksList
    }

    // traverse helper
    private fun traverseExprRec(
        expr: ExpressionDto, // current expression
        tasksList: MutableList<TaskHolder>,
        resTo: CompletableDeferred<TableDto>? = null, // where to put result after finishing
        argsList: List<TableDto> // list of args from ExpressionHolder
    ) {
        if (expr.leftExpr == null && expr.rightExpr == null) {
            // both args are regular tables
            val newTask =
                TaskHolder(
                    expr.operation,
                    expr.leftTableIndex?.let { argsList[it] },
                    expr.rightTableIndex?.let { argsList[it] },
                    resultTo = resTo
                )
            tasksList.add(newTask)
        } else {
            // at least one arg is expression, has to be computed earlier
            val leftDeferred = expr.leftExpr?.let {
                val deferred = CompletableDeferred<TableDto>()
                traverseExprRec(it, tasksList, deferred, argsList)
                deferred
            }
            val rightDeferred = expr.rightExpr?.let {
                val deferred = CompletableDeferred<TableDto>()
                traverseExprRec(it, tasksList, deferred, argsList)
                deferred
            }
            val newTask = TaskHolder(
                expr.operation,
                expr.leftTableIndex?.let { argsList[it] },
                expr.rightTableIndex?.let { argsList[it] },
                leftDeferred,
                rightDeferred,
                resTo
            )

            tasksList.add(newTask)
        }
    }
}
