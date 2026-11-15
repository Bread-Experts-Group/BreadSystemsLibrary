package org.bread_experts_group.bread_systems_library.model.matrix.status.data

import org.bread_experts_group.bread_systems_library.model.JSONObject
import org.bread_experts_group.bread_systems_library.model.JSONPrimitive
import org.bread_experts_group.bread_systems_library.model.codings.JSONName
import org.bread_experts_group.bread_systems_library.model.matrix.MatrixID
import java.math.BigInteger

data class MatrixSynchronization(
    @JSONName("account_data") val accountData: AccountData? = null,
    @JSONName("device_lists") val deviceLists: DeviceLists? = null,
    @JSONName("device_one_time_keys_count") val deviceOneTimeKeysCount: Map<String, BigInteger>? = null,
    @JSONName("next_batch") val nextBatch: String,
    val presence: Presence? = null,
    val rooms: Rooms? = null,
    @JSONName("to_device") val toDevice: ToDevice? = null
) : MatrixData {
    value class RoomID(val value: JSONPrimitive?)
    value class EventID(val value: JSONPrimitive?)

    data class DeviceLists(
        val changed: List<String>? = null,
        val left: List<String>? = null
    )

    data class ToDevice(
        val events: List<Event>? = null
    )

    data class Event(
        val content: JSONObject,
        val type: String
    )

    data class StrippedStateEvent(
        val content: JSONObject,
        val sender: MatrixID,
        @JSONName("state_key") val stateKey: String,
        val type: String
    )

    data class AccountData(
        val events: List<Event>
    )

    data class Presence(
        val events: List<Event>
    )

    data class State(val events: List<ClientEventWithoutRoomID>) {
        data class ClientEventWithoutRoomID(
            val content: JSONObject,
            @JSONName("event_id") val eventID: EventID,
            @JSONName("origin_server_ts") val originServerTimestamp: BigInteger,
            val sender: MatrixID,
            @JSONName("state_key") val stateKey: String? = null,
            val type: String,
            val unsigned: UnsignedData? = null
        ) {
            data class UnsignedData(
                val age: BigInteger? = null,
                val membership: String? = null,
                @JSONName("prev_content") val previousContent: JSONObject? = null,
                @JSONName("redacted_because") val redactedBecause: ClientEventWithoutRoomID? = null,
                @JSONName("replaces_state") val replacesState: EventID? = null,
                @JSONName("transaction_id") val transactionID: String? = null,
            )
        }
    }

    data class Timeline(
        val events: List<State.ClientEventWithoutRoomID>,
        val limited: Boolean? = null,
        @JSONName("prev_batch") val previousBatch: String? = null
    )

    data class Rooms(
        val invite: Map<RoomID, InvitedRoom>? = emptyMap(),
        val join: Map<RoomID, JoinedRoom>? = emptyMap(),
        val knock: Map<RoomID, KnockedRoom>? = emptyMap(),
        val leave: Map<RoomID, LeftRoom>? = emptyMap()
    ) {
        data class InvitedRoom(@JSONName("invite_state") val inviteState: InviteState) {
            data class InviteState(val events: List<StrippedStateEvent>)
        }

        data class JoinedRoom(
            @JSONName("account_data") val accountData: AccountData? = null,
            val ephemeral: Ephemeral? = null,
            val state: State? = null,
            @JSONName("state_after") val stateAfter: State? = null,
            val summary: RoomSummary? = null,
            val timeline: Timeline? = null,
            @JSONName("unread_notifications") val unreadNotificationCounts: UnreadNotificationCounts? = null,
            @JSONName("unread_thread_notifications") val unreadThreadNotifications: Map<EventID, UnreadThreadNotifications>? = null,
        ) {
            data class Ephemeral(
                val events: List<Event>
            )

            data class RoomSummary(
                @JSONName("m.heroes") val mHeroes: List<MatrixID>? = null,
                @JSONName("m.invited_member_count") val invitedMemberCount: BigInteger? = null,
                @JSONName("m.joined_member_count") val joinedMemberCount: BigInteger? = null,
            )

            data class UnreadNotificationCounts(
                @JSONName("highlight_count") val highlightCount: BigInteger? = null,
                @JSONName("notification_count") val notificationCount: BigInteger? = null
            )

            data class UnreadThreadNotifications(
                @JSONName("highlight_count") val highlightCount: BigInteger? = null,
                @JSONName("notification_count") val notificationCount: BigInteger? = null
            )
        }

        data class KnockedRoom(
            @JSONName("knock_state") val knockState: KnockState? = null
        ) {
            data class KnockState(
                val events: List<StrippedStateEvent>
            )
        }

        data class LeftRoom(
            @JSONName("account_data") val accountData: AccountData? = null,
            val state: State? = null,
            @JSONName("state_after") val stateAfter: State? = null,
            val timeline: Timeline? = null
        )
    }
}