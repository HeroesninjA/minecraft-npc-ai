package ro.ainpc.gui;

@FunctionalInterface
public interface GuiAction {
    void execute(GuiClickContext context);
}
