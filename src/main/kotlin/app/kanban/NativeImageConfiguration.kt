package app.kanban

import app.kanban.kanban.CardInsertResult
import app.kanban.kanban.ColumnInfo
import app.kanban.kanban.KanbanBoard
import app.kanban.kanban.KanbanCard
import app.kanban.kanban.KanbanOwnerShip
import app.kanban.security.KanbanUser
import app.kanban.user.Identifier
import app.kanban.user.IdentifierType
import app.kanban.web.data.CardMovement
import app.kanban.web.data.KanbanCardWeb
import app.kanban.web.data.KanbanWeb
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User

@Configuration
@ImportRuntimeHints(NativeImageHints::class)
class NativeImageConfiguration

class NativeImageHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        // Reflection hints for domain classes - Kanban
        registerReflection(hints, KanbanUser::class.java)
        registerReflection(hints, Identifier::class.java)
        registerReflection(hints, IdentifierType::class.java)
        registerReflection(hints, KanbanBoard::class.java)
        registerReflection(hints, KanbanCard::class.java)
        registerReflection(hints, KanbanOwnerShip::class.java)
        registerReflection(hints, CardInsertResult::class.java)
        registerReflection(hints, ColumnInfo::class.java)
        
        // Web data classes
        registerReflection(hints, KanbanWeb::class.java)
        registerReflection(hints, KanbanCardWeb::class.java)
        registerReflection(hints, CardMovement::class.java)
        
        // Reflection hints for Spring Security OAuth2 classes
        hints.reflection().registerType(OAuth2User::class.java) { hint ->
            hint.withMembers(
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
            )
        }
        
        hints.reflection().registerType(GrantedAuthority::class.java) { hint ->
            hint.withMembers(
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
            )
        }
        
        hints.reflection().registerType(SecurityContext::class.java) { hint ->
            hint.withMembers(
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
            )
        }
        
        hints.reflection().registerType(OAuth2LoginAuthenticationToken::class.java) { hint ->
            hint.withMembers(
                org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
            )
        }
        
        // JTE templates hints - library classes
        val templateClasses = listOf(
            "gg.jte.TemplateEngine",
            "gg.jte.ContentType",
            "gg.jte.Template",
            "gg.jte.TemplateOutput",
            "gg.jte.runtime.TemplateLoader",
            "gg.jte.runtime.Template",
            "gg.jte.spring.boot.autoconfigure.JteAutoConfiguration",
            "gg.jte.spring.boot.autoconfigure.JteProperties",
            "gg.jte.spring.boot.autoconfigure.JteViewResolver"
        )

        templateClasses.forEach { className ->
            try {
                val clazz = Class.forName(className, false, classLoader)
                hints.reflection().registerType(clazz) { hint ->
                    hint.withMembers(
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS
                    )
                }
            } catch (e: ClassNotFoundException) {
                // Class not available, skip
            }
        }

        // JTE generated precompiled template classes - these are critical for native image
        val jteGeneratedClasses = listOf(
            "gg.jte.generated.precompiled.JtekanbanGenerated",
            "gg.jte.generated.precompiled.JtecolumnsGenerated",
            "gg.jte.generated.precompiled.JteconfigureColumnsGenerated",
            "gg.jte.generated.precompiled.JtecolumnGenerated",
            "gg.jte.generated.precompiled.JtecolumnBoxGenerated",
            "gg.jte.generated.precompiled.JtecardGenerated",
            "gg.jte.generated.precompiled.JtecardMoveUpdateGenerated",
            "gg.jte.generated.precompiled.JtenavbarGenerated",
            "gg.jte.generated.precompiled.JtesidebarGenerated",
            "gg.jte.generated.precompiled.JtekanbansGenerated",
            "gg.jte.generated.precompiled.JtenewColumnBoxGenerated",
            "gg.jte.generated.precompiled.JterootGenerated",
            "gg.jte.generated.precompiled.JtetitleUpdateGenerated"
        )

        jteGeneratedClasses.forEach { className ->
            hints.reflection().registerType(TypeReference.of(className)) { hint ->
                hint.withMembers(
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS
                )
            }
        }
        
        // Resource hints for Flyway migrations and JTE templates
        hints.resources().registerPattern("db/migration/*")
        hints.resources().registerPattern("db/migration/**")
        hints.resources().registerPattern("jte/*")
        hints.resources().registerPattern("gg/jte/generated/*")
        hints.resources().registerPattern("static/css/*")
        hints.resources().registerPattern("static/js/*")
        hints.resources().registerPattern("*.properties")
        hints.resources().registerPattern("*.yml")
        hints.resources().registerPattern("*.yaml")
        
        // Serialization hints - only for classes that are actually serialized (session storage)
        hints.serialization().registerType(KanbanUser::class.java)
        hints.serialization().registerType(TypeReference.of("java.util.ArrayList"))
        hints.serialization().registerType(TypeReference.of("java.util.HashMap"))
        hints.serialization().registerType(TypeReference.of("java.util.Collections\$UnmodifiableMap"))
        hints.serialization().registerType(TypeReference.of("java.util.Collections\$EmptyList"))
        hints.serialization().registerType(TypeReference.of("java.util.Collections\$SingletonSet"))
        hints.serialization().registerType(TypeReference.of("java.util.LinkedHashSet"))
        hints.serialization().registerType(TypeReference.of("java.lang.Long"))
        hints.serialization().registerType(TypeReference.of("java.lang.String"))
        hints.serialization().registerType(TypeReference.of("java.lang.Integer"))
        
        // Time classes for JDBC serialization
        hints.serialization().registerType(TypeReference.of("java.time.Instant"))
    }
    
    private fun registerReflection(hints: RuntimeHints, clazz: Class<*>) {
        hints.reflection().registerType(clazz) { hint ->
            hint.withMembers(
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS
            )
        }
    }
}
