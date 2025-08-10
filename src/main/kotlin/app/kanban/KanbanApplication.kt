package app.kanban

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KanbanApplication

fun main(args: Array<String>) {
	runApplication<KanbanApplication>(*args)
}
