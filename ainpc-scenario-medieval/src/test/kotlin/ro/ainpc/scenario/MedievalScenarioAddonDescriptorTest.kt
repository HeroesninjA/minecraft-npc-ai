package ro.ainpc.scenario

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.addons.AddonType
import ro.ainpc.addons.medieval.MedievalScenarioAddon

class MedievalScenarioAddonDescriptorTest {
    @Test
    fun medievalAddonDeclaresScenarioTypeAndCapability() {
        val descriptor = MedievalScenarioAddon("test").getDescriptor()

        assertEquals(AddonType.SCENARIO, descriptor.type)
        assertTrue(descriptor.isPrimaryScenario)
        assertTrue(descriptor.capabilities.contains("scenarios"))
    }
}
