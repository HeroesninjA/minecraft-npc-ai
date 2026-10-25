package ro.ainpc.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionNodeJavaInteropTest {
    @Test
    void javaCanStillResolveExistingPermissionNamesAndAccessors() {
        assertSame(PermissionNode.TALK, PermissionNode.valueOf("TALK"));
        assertSame(PermissionNode.INFO, PermissionNode.valueOf("INFO"));
        assertEquals("ainpc.talk", PermissionNode.TALK.getNode());
        assertEquals(PermissionLevel.PLAY, PermissionNode.TALK.getLevel());
        assertEquals("true", PermissionNode.TALK.getDefaultValue());
        assertEquals("ainpc.info", PermissionNode.INFO.getNode());
        assertEquals(PermissionLevel.VIEW, PermissionNode.INFO.getLevel());
        assertSame(PermissionNode.TALK, PermissionNode.Companion.fromNode("ainpc.talk"));
        assertSame(PermissionNode.INFO, PermissionNode.Companion.fromNode("ainpc.info"));
    }

    @Test
    void javaCanResolveTheAdditionalProfilePermission() {
        assertSame(PermissionNode.PROFILE, PermissionNode.valueOf("PROFILE"));
        assertEquals("ainpc.profile", PermissionNode.PROFILE.getNode());
        assertEquals(PermissionLevel.PLAY, PermissionNode.PROFILE.getLevel());
        assertEquals("true", PermissionNode.PROFILE.getDefaultValue());
        assertSame(PermissionNode.PROFILE, PermissionNode.Companion.fromNode("ainpc.profile"));
        assertTrue(PermissionNode.Companion.allNodes().contains("ainpc.profile"));
        assertTrue(PermissionNode.Companion.nodesByLevel(PermissionLevel.PLAY).contains(PermissionNode.PROFILE));
    }
}
