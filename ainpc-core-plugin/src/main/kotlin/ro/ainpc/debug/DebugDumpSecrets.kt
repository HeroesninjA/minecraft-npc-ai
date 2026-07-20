package ro.ainpc.debug

import ro.ainpc.context.SensitiveDataRedactionPolicy
import ro.ainpc.context.SensitiveDataRedactor

object DebugDumpSecrets {
    private val POLICY = SensitiveDataRedactionPolicy.secretsOnly()

    @JvmStatic
    fun redactText(rawText: String?): String = SensitiveDataRedactor.redact(rawText, POLICY)

    @JvmStatic
    fun containsPotentialSecret(rawText: String?): Boolean =
        SensitiveDataRedactor.containsPotentialSensitiveData(rawText, POLICY)
}
