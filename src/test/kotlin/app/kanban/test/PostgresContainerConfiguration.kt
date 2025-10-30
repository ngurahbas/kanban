package app.kanban.test

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresContainerConfiguration : BeforeAllCallback {
    
    companion object {
        private const val IMAGE_VERSION = "postgres:15-alpine"
        private const val DATABASE_NAME = "kanban_test"
        private const val USERNAME = "kanban_test"
        private const val PASSWORD = "kanban_test"
        
        @JvmStatic
        private val postgresContainer: PostgreSQLContainer<*> = 
            PostgreSQLContainer(DockerImageName.parse(IMAGE_VERSION))
                .withDatabaseName(DATABASE_NAME)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
    }
    
    override fun beforeAll(context: ExtensionContext) {
        if (!postgresContainer.isRunning) {
            postgresContainer.start()
            
            // Set system properties for the test configuration
            System.setProperty("spring.datasource.url", postgresContainer.jdbcUrl)
            System.setProperty("spring.datasource.username", postgresContainer.username)
            System.setProperty("spring.datasource.password", postgresContainer.password)
        }
    }
    
    fun getContainer(): PostgreSQLContainer<*> = postgresContainer
}
