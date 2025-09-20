package app.kanban

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class KanbanApplication

fun main(args: Array<String>) {
    runApplication<KanbanApplication>(*args)
}
