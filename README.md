# Distributed relational algebra
Distributed app on Kotlin that uses [RabbitMQ](https://www.rabbitmq.com/) to calculate simple relational algebra expressions (currently only set theory and cartesian product operations are available). Client part also provides DSL for creating tables and forming realtional algebra expressions.

## App structure
App consists of three main parts:
- [Client](Client/src/main/kotlin/) where user can create tables and form realtional algebra expressions using DSL that are sent to manager for calculating
- [Manager](Manager/src/main/kotlin/) that receives expressions to calculate from Client, loads them to queue for Workers, waits for result and sends back to Client
- [Worker](Worker/src/main/kotlin/) that receives atomic tasks (only one operations with two tables) from RabbitMQ queue and sends replies to corresponding reply queues (multiple Workers can run at the same time)

Manager is powered by [KTor](https://ktor.io/) and uses web sockets to interact with Client: socket is opened when Client wants to calculate new expression, closed when the result (successful or exceptional) was sent back to Client or if Client canceles the current task.

Manager and Worker use RabbitMQ to interact with each other.

## Installation
To install app, download [install](install) directory to your machine (the directory contains jars for Manager and Worker and dockerfiles) and use docker compose command in downloaded directory:
```Shell
docker compose up
```

After that RabbitMQ, Worker(s) and Manager will run in separate containers. Number of Workers can be specified using deploy-replicas option in [docker-compose](install/docker-compose.yml) file.

After successful installation Client module can be used to run custom code for performing relational algebra calculations.

## Examples
Usage examples are provided in [Client demo](Client/src/main/kotlin/Demo.kt) file. 

Creating tables and inserting data
```kotlin
val t1 = Table("users1") {
        column<Int>("id")
        column<String>("email")
        column<LocalDateTime>("hire_date")
    }.insert {
        (1..9).map {
            listOf<Any>( it, "$it@mail.com", LocalDateTime.parse("2023-01-0$it 00:00:00", formatter))
        }
    }
 ```
 
 Constructing expression and sending it to Manager
 ```kotlin
 val expr = Expression {
        (t1 intersect t2) intersect (t2 union t3) union (t1 intersect t2 intersect t3) product t2
    }
 val res = expr.compute().get().asTable()
 ```
 
