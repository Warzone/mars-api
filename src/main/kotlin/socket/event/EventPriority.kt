package network.warzone.api.socket.event

enum class EventPriority(position: Int) {
    EARLIEST(100),
    EARLY(75),
    LATE(50),
    LATEST(25),
    MONITOR(0)
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FireAt(val priority: EventPriority)