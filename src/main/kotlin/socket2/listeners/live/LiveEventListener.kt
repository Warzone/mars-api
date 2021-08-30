package network.warzone.api.socket2.listeners.live

//import network.warzone.api.socket2.event.EventPriority
//import network.warzone.api.socket2.event.FireAt
//import network.warzone.api.socket2.event.Listener
//import network.warzone.api.socket2.event.ServerEvent
//import network.warzone.api.socket2.listeners.match.MatchEndEvent
//import network.warzone.api.socket2.listeners.match.MatchLoadEvent
//import network.warzone.api.socket2.listeners.match.MatchStartEvent
//import kotlin.reflect.KClass
//import kotlin.reflect.KFunction1

/*
 * This listener is responsible for handling socket events and transforming them into game-scope events ("Live Events") when applicable
 * Live Events are events that may be displayed in a "replay feed" of the match, e.g. Match Start, Player Death, Flag Capture
 */
//class LiveEventListener : Listener() {
//    override val handlers = mapOf(
//        ::onMatchLoad to MatchLoadEvent::class,
//        ::onMatchStart to MatchStartEvent::class,
//        ::onMatchEnd to MatchEndEvent::class
//    )
//
//    @FireAt(EventPriority.LATE)
//    fun onMatchLoad(event: MatchLoadEvent) {
//
//    }
//
//    @FireAt(EventPriority.LATE)
//    fun onMatchStart(event: MatchStartEvent) {
//
//    }
//
//    @FireAt(EventPriority.LATE)
//    fun onMatchEnd(event: MatchEndEvent) {
//
//    }
//}