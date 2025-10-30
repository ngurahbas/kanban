package app.kanban.kanban

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [KanbanServiceCacheTest.TestConfig::class])
class KanbanServiceCacheTest {

    @Autowired
    lateinit var kanbanService: KanbanService

    @Autowired
    lateinit var boardRepository: KanbanBoardRepository

    @Autowired
    lateinit var cacheManager: CacheManager

    @BeforeEach
    fun clearCache() {
        cacheManager.getCache("kanbanColumns")?.clear()
        Mockito.reset(boardRepository)
    }

    @Test
    fun `getColumns is cached and evicted on update`() {
        val boardId = 1L
        val columns1 = linkedSetOf("Todo", "Doing")
        val columns2 = linkedSetOf("Todo", "Done")

        Mockito.`when`(boardRepository.findColumns(boardId)).thenReturn(columns1, columns2)

        val first = kanbanService.getColumns(boardId)
        val second = kanbanService.getColumns(boardId)
        assertEquals(columns1, first)
        assertEquals(columns1, second)
        Mockito.verify(boardRepository, times(1)).findColumns(boardId)

        // Evict cache by updating columns
        kanbanService.updateColumns(boardId, columns2)

        val afterEvict = kanbanService.getColumns(boardId)
        assertEquals(columns2, afterEvict)
        Mockito.verify(boardRepository, times(2)).findColumns(boardId)
    }

    @TestConfiguration
    @EnableCaching
    class TestConfig {
        @Bean
        fun kanbanBoardRepository(): KanbanBoardRepository = Mockito.mock(KanbanBoardRepository::class.java)

        @Bean
        fun kanbanCardRepository(): KanbanCardRepository = Mockito.mock(KanbanCardRepository::class.java)

        @Bean
        fun kanbanOwnershipRepository(): KanbanOwnershipRepository = Mockito.mock(KanbanOwnershipRepository::class.java)

        @Bean
        fun kanbanService(
            boardRepository: KanbanBoardRepository,
            cardRepository: KanbanCardRepository,
            ownershipRepository: KanbanOwnershipRepository
        ): KanbanService = KanbanService(boardRepository, cardRepository, ownershipRepository)

        @Bean
        fun cacheManager(): CacheManager = ConcurrentMapCacheManager("kanbanColumns")
    }
}
