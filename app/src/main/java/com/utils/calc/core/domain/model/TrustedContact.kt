package com.utils.calc.core.domain.model

/**
 * Contato de confianca. O telefone e' apenas armazenado e exibido — nesta fase do
 * projeto nenhuma chamada ou mensagem e' feita a partir dele (ver CLAUDE.md).
 */
data class TrustedContact(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val isPrimary: Boolean,
) {
    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
}
