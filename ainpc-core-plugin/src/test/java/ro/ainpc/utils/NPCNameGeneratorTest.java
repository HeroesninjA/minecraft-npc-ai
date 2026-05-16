package ro.ainpc.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NPCNameGeneratorTest {

    @Test
    void predefinedNamePoolContainsHundredsOfUniqueNames() {
        var maleNames = NPCNameGenerator.predefinedNames("male");
        var femaleNames = NPCNameGenerator.predefinedNames("female");
        var allNames = new HashSet<String>();
        allNames.addAll(maleNames);
        allNames.addAll(femaleNames);

        assertTrue(NPCNameGenerator.predefinedNameCount() >= 300);
        assertEquals(NPCNameGenerator.predefinedNameCount(), allNames.size());
    }
}
