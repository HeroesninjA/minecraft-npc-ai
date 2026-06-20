package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScriptDocumentNormalizerTest {
    @Test
    fun normalizesYamlAndJsonToSameTree() {
        val yaml = """
            type: quest
            version: 1
            spec:
              trigger:
                - quest.started
                - quest.finished
              target:
                kind: place
                ref: tag:market
        """.trimIndent()

        val json = """
            {
              "type": "quest",
              "version": 1,
              "spec": {
                "trigger": ["quest.started", "quest.finished"],
                "target": {
                  "kind": "place",
                  "ref": "tag:market"
                }
              }
            }
        """.trimIndent()

        val yamlDocument = ScriptDocumentNormalizer.parseDocument(yaml, "quest.yaml")
        val jsonDocument = ScriptDocumentNormalizer.parseDocument(json, "quest.json")

        assertEquals(jsonDocument, yamlDocument)
    }

    @Test
    fun wrapsInvalidDocumentsWithErrorInformation() {
        val document = ScriptDocumentNormalizer.normalizeDocument("{not: valid", "quest.json")

        assertTrue(document.has("available"))
        assertTrue(document.has("error"))
    }
}
