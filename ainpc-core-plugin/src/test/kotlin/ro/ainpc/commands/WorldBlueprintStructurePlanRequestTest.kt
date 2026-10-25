package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.WorldBlueprintRegionBounds

class WorldBlueprintStructurePlanRequestTest {
    private val legacyArgs = arrayOf("world", "blueprint", "lumea", "plan", "structure", "house", "house_a")
    private val boundsArgs = legacyArgs + arrayOf("bounds", "-2", "64", "-3", "2", "68", "3")
    private val fingerprint = "0123456789abcdef".repeat(4)

    @Test
    fun parsesLegacyStructurePlanWithoutBounds() {
        assertEquals(
            WorldBlueprintStructurePlanRequest("house", "house_a", null, null),
            WorldBlueprintStructurePlanRequest.parse(legacyArgs)
        )
    }

    @Test
    fun parsesLegacyStructurePlanWithBounds() {
        assertEquals(
            WorldBlueprintStructurePlanRequest("house", "house_a", WorldBlueprintRegionBounds(-2, 64, -3, 2, 68, 3), null),
            WorldBlueprintStructurePlanRequest.parse(boundsArgs)
        )
    }

    @Test
    fun parsesExportSuffixAndNormalizesFingerprint() {
        assertEquals(
            WorldBlueprintStructurePlanRequest(
                "house", "house_a", WorldBlueprintRegionBounds(-2, 64, -3, 2, 68, 3), fingerprint
            ),
            WorldBlueprintStructurePlanRequest.parse(boundsArgs + arrayOf("export", fingerprint.uppercase()))
        )
    }

    @Test
    fun parsesVerificationSuffixWithoutRequestingExport() {
        val request = WorldBlueprintStructurePlanRequest.parse(boundsArgs + arrayOf("VeRiFy", fingerprint.uppercase()))

        assertEquals(WorldBlueprintStructurePlanRequest("house", "house_a",
            WorldBlueprintRegionBounds(-2, 64, -3, 2, 68, 3), null, fingerprint), request)
        assertNull(request?.exportFingerprint)
    }

    @Test
    fun acceptsMixedCaseKeywordsAndPreservesIdentifiers() {
        val args = arrayOf(
            "WoRlD", "BlUePrInT", "Lumea", "PlAn", "StRuCtUrE", "House", "House_A",
            "BoUnDs", "-2", "64", "-3", "2", "68", "3", "ExPoRt", fingerprint
        )
        val request = WorldBlueprintStructurePlanRequest.parse(args)

        assertEquals("House", request?.typeId)
        assertEquals("House_A", request?.baseId)
        assertEquals(fingerprint, request?.exportFingerprint)
    }

    @Test
    fun rejectsUnsupportedShapesAndTrailingArguments() {
        val requests = listOf(
            emptyArray(),
            legacyArgs.dropLast(1).toTypedArray(),
            legacyArgs + "bounds",
            legacyArgs + arrayOf("export", fingerprint),
            legacyArgs + arrayOf("verify", fingerprint),
            boundsArgs.dropLast(1).toTypedArray(),
            boundsArgs + "export",
            boundsArgs + arrayOf("export", fingerprint, "extra"),
            boundsArgs + arrayOf("export", fingerprint.take(32), fingerprint.drop(32)),
            boundsArgs + "verify",
            boundsArgs + arrayOf("verify", fingerprint, "extra"),
            boundsArgs + arrayOf("verify", fingerprint.take(32), fingerprint.drop(32)),
            boundsArgs + arrayOf("verify", fingerprint, "export", fingerprint),
            boundsArgs + arrayOf("export", fingerprint, "verify", fingerprint)
        )

        requests.forEach { args ->
            assertNull(WorldBlueprintStructurePlanRequest.parse(args), args.joinToString(" "))
        }
    }

    @Test
    fun rejectsUnsupportedKeywordsAndSuffixOrdering() {
        for (keywordIndex in listOf(0, 1, 3, 4, 7, 14)) {
            val args = boundsArgs + arrayOf("export", fingerprint)
            args[keywordIndex] = "unknown"
            assertNull(WorldBlueprintStructurePlanRequest.parse(args), "keyword index $keywordIndex")
        }
        assertNull(
            WorldBlueprintStructurePlanRequest.parse(
                legacyArgs + arrayOf("export", fingerprint, "bounds", "-2", "64", "-3", "2", "68", "3")
            )
        )
        assertNull(WorldBlueprintStructurePlanRequest.parse(
            legacyArgs + arrayOf("verify", fingerprint, "bounds", "-2", "64", "-3", "2", "68", "3")))
    }

    @Test
    fun rejectsMalformedAndOverflowingCoordinatesInSupportedShapes() {
        for (invalidCoordinate in listOf("invalid", "64.5", "2147483648", "-2147483649")) {
            for (suffix in listOf(emptyArray(), arrayOf("export", fingerprint), arrayOf("verify", fingerprint))) {
                val args = boundsArgs + suffix
                args[8] = invalidCoordinate
                val failure = assertThrows(IllegalArgumentException::class.java) {
                    WorldBlueprintStructurePlanRequest.parse(args)
                }
                assertTrue(failure.message.orEmpty().contains("Coordonata"))
            }
        }
    }

    @Test
    fun rejectsReversedBoundsWithUsefulMessage() {
        val args = boundsArgs.copyOf()
        args[8] = "3"

        val failure = assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStructurePlanRequest.parse(args)
        }

        assertTrue(failure.message.orEmpty().contains("Coordonatele minime"))
    }

    @Test
    fun rejectsIncompleteNonHexAndWhitespaceFingerprints() {
        val invalidFingerprints = listOf(
            "", fingerprint.take(63), fingerprint + "0", "g".repeat(64), "\uFF41".repeat(64),
            " $fingerprint", "$fingerprint ", "${fingerprint.take(63)}\n"
        )
        invalidFingerprints.forEach { invalidFingerprint ->
            for (action in listOf("export", "verify")) {
                val failure = assertThrows(IllegalArgumentException::class.java) {
                    WorldBlueprintStructurePlanRequest.parse(boundsArgs + arrayOf(action, invalidFingerprint))
                }
                assertTrue(failure.message.orEmpty().contains("64 de caractere hexazecimale"))
            }
        }
    }
}
