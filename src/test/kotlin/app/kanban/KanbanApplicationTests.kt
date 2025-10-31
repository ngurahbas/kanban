package app.kanban

import app.kanban.test.PostgresContainerConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@ExtendWith(PostgresContainerConfiguration::class)
@SpringBootTest
class KanbanApplicationTests {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
	fun contextLoads() {
	}
    
    @Test
    fun `is using test container`() {
        val dbName = jdbcTemplate.queryForObject("SELECT current_database()", String::class.java)
        assertEquals("kanban_test", dbName)
    }

}
