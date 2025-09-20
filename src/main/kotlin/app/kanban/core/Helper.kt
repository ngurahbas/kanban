package app.kanban.core

import com.fasterxml.jackson.databind.ObjectMapper

fun toJsonString(obj: Any): String {
    return ObjectMapper().writeValueAsString(obj)
}