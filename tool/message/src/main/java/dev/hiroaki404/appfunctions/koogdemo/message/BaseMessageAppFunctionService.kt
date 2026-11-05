package dev.hiroaki404.appfunctions.koogdemo.message

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    //  describe here that this takes a name returned by it, and let the LLM map honorifics or
    //  nicknames to the closest one. Kept out of the KDoc on purpose, since the KDoc is sent to the LLM.
    /**
     * Sends a text message to one of the contacts saved in this app. The message is only recorded
     * in the app's history and shown on screen; nothing is delivered to a real person.
     *
     * @param contactName The recipient. Honorifics and nicknames correspond to the closest of these
     *   names; for example, "トムくん" corresponds to "トム".
     * @param messageBody The message body, which can't be blank.
     * @return A short confirmation such as "トムに送信しました".
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun sendMessage(
        @AppFunctionStringValueConstraint(enumValues = ["トム", "花子", "健太", "Tom", "Hanako", "Kenta"])
        contactName: String,
        messageBody: String,
    ): String = executeLogged(
        "sendMessage",
        "contactName=$contactName, messageBody=$messageBody",
    ) {
        val contact = resolveContact(contactName)
            ?: throw AppFunctionElementNotFoundException(
                "No contact matches '$contactName'. Available contacts are ${availableContacts()}.",
            )
        if (messageBody.isBlank()) {
            throw AppFunctionInvalidArgumentException(
                "messageBody must not be blank. Provide the message body to send to ${contact.name}.",
            )
        }
        messageRepository.record(contact, messageBody.trim())
        "${contact.name}に送信しました"
    }

    private fun availableContacts(): String = demoContacts.joinToString { contact ->
        (listOf(contact.name) + contact.aliases).joinToString(separator = " / ")
    }

    private suspend fun <T : Any> executeLogged(
        functionName: String,
        arguments: String,
        block: suspend () -> T,
    ): T = withContext(Dispatchers.IO) {
        appFunctionLogger.started(functionName, arguments)
        try {
            block().also { result -> appFunctionLogger.succeeded(functionName, arguments, result) }
        } catch (error: Exception) {
            appFunctionLogger.failed(functionName, arguments, error)
            throw error
        }
    }
}
