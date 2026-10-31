package dev.hiroaki404.appfunctions.koogdemo.message

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

/**
 * Exposes the messaging app to AppFunctions callers.
 *
 * The compiler generates a concrete `MessageAppFunctionService` (declared in AndroidManifest.xml)
 * plus an XML describing the exposed functions.
 */
@AppFunctionServiceEntryPoint(
    serviceName = "MessageAppFunctionService",
    appFunctionXmlFileName = "message_functions",
)
abstract class BaseMessageAppFunctionService : AppFunctionService() {
    /** Override in tests or alternate hosts to observe calls without Android logcat. */
    protected open val appFunctionLogger: AppFunctionLogger = AndroidAppFunctionLogger

    /** Override in tests or alternate hosts to avoid depending on [MessageApplication]. */
    protected open val messageRepository: MessageRepository
        get() = (application as MessageApplication).messageRepository

    // TODO: The contact names below are hardcoded copies of demoContacts so the LLM knows the valid
    //  recipients. A real messaging app has per-user, changing contacts that can't be written into a
    //  build-time description: expose them through an AppFunction (e.g. listContacts/searchContacts),
    //  tell the LLM here to pass a name returned by it, and let the LLM map honorifics or nicknames to
    //  the closest one. Kept out of the KDoc on purpose, since the KDoc is sent to the LLM.
    /**
     * Send a text message to one of the contacts saved in this app.
     *
     * The message is only recorded in the app's history and shown on screen; nothing is
     * delivered to a real person.
     *
     * @param contactName The recipient, exactly as one of the listed names: トム, 花子, 健太, or
     *   their English names Tom, Hanako, Kenta. If the user refers to a contact with an honorific
     *   or a nickname (for example "トムくん"), pass the closest listed name instead (here "トム").
     * @param text The message body. It must not be blank.
     * @return A short result such as "トムに送信しました".
     * @throws AppFunctionInvalidArgumentException If the contact is not one of the available
     *   contacts or the text is blank. Correct the contact name or text and retry.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun sendMessage(contactName: String, text: String): String = executeLogged(
        "sendMessage",
        "contactName=$contactName, text=$text",
    ) {
        val contact = resolveContact(contactName)
            ?: throw AppFunctionInvalidArgumentException(
                "Unknown contact: '$contactName'. Available contacts are ${availableContacts()}.",
            )
        if (text.isBlank()) {
            throw AppFunctionInvalidArgumentException(
                "text must not be blank. Provide the message body to send to ${contact.name}.",
            )
        }
        messageRepository.record(contact, text.trim())
        "${contact.name}に送信しました"
    }

    private fun availableContacts(): String = demoContacts.joinToString { contact ->
        (listOf(contact.name) + contact.aliases).joinToString(separator = " / ")
    }

    private inline fun <T : Any> executeLogged(
        functionName: String,
        arguments: String,
        block: () -> T,
    ): T {
        appFunctionLogger.started(functionName, arguments)
        return try {
            block().also { result -> appFunctionLogger.succeeded(functionName, arguments, result) }
        } catch (error: Exception) {
            appFunctionLogger.failed(functionName, arguments, error)
            throw error
        }
    }
}
