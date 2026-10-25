package dev.hiroaki404.appfunctions.koogdemo.message


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

/**
 * Finds the contact whose name or alias equals [input], ignoring surrounding whitespace and the case
 * of Latin letters. Returns null when no contact matches.
 *
 * Honorifics and nicknames (e.g. "トムくん") are deliberately not handled here: the sendMessage
 * description asks the calling LLM to map them to a listed name.
 */
fun resolveContact(input: String, contacts: List<Contact> = demoContacts): Contact? {
    val name = input.trim()
    if (name.isEmpty()) return null
    return contacts.firstOrNull { contact ->
        (listOf(contact.name) + contact.aliases).any { it.equals(name, ignoreCase = true) }
    }
}
