package dev.hiroaki404.appfunctions.koogdemo.message

import java.util.Locale

/** A fixed contact the demo app can send messages to. */
data class Contact(
    val name: String,
    /** Alternative spellings that also resolve to this contact, e.g. an English name. */
    val aliases: List<String> = emptyList(),
)

val demoContacts = listOf(
    Contact("トム", aliases = listOf("Tom")),
    Contact("花子", aliases = listOf("Hanako")),
    Contact("健太", aliases = listOf("Kenta")),
)

private val honorifics = listOf("くん", "君", "さん", "ちゃん", "様", "さま")

/**
 * Resolves a name as spoken by a user (e.g. "トムくん", " tom ") to a contact.
 *
 * Surrounding whitespace and one trailing honorific are ignored, and Latin letters are compared
 * case-insensitively. Returns null when no contact matches.
 */
fun resolveContact(input: String, contacts: List<Contact> = demoContacts): Contact? {
    val normalized = normalizeContactName(input)
    if (normalized.isEmpty()) return null
    return contacts.firstOrNull { contact ->
        (listOf(contact.name) + contact.aliases).any { normalizeContactName(it) == normalized }
    }
}

private fun normalizeContactName(name: String): String {
    val trimmed = name.trim()
    val honorific = honorifics.firstOrNull { trimmed.length > it.length && trimmed.endsWith(it) }
    val withoutHonorific = if (honorific == null) trimmed else trimmed.removeSuffix(honorific).trim()
    return withoutHonorific.lowercase(Locale.ROOT)
}
