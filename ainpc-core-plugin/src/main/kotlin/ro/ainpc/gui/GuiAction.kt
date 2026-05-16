package ro.ainpc.gui

fun interface GuiAction {
    fun execute(context: GuiClickContext)
}
