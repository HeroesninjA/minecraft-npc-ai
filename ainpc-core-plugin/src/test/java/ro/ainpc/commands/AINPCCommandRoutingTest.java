package ro.ainpc.commands;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AINPCCommandRoutingTest {

    @Test
    void routesProgressionAliasGuiToKindFilter() throws Exception {
        AINPCCommand command = new AINPCCommand(null);
        Method route = AINPCCommand.class.getDeclaredMethod("routeProgressionAlias", String[].class, String.class);
        route.setAccessible(true);

        assertArrayEquals(
            new String[] {"quest", "gui", "contract"},
            (String[]) route.invoke(command, new String[] {"contract", "gui"}, "contract")
        );
        assertArrayEquals(
            new String[] {"quest", "gui", "contract_active"},
            (String[]) route.invoke(command, new String[] {"contract", "gui", "active"}, "contract")
        );
        assertArrayEquals(
            new String[] {"quest", "gui", "ritual_tracked"},
            (String[]) route.invoke(command, new String[] {"ritual", "gui", "tracked"}, "ritual")
        );
    }
}
