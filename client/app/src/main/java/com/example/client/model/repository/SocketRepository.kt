package com.example.client.model.repository

import com.example.client.model.data.ChatRoom
import com.example.client.model.data.Message
import com.example.client.model.data.User
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.concurrent.ConcurrentHashMap

/**
 * SocketRepository implemented on top of socket.io-client.
 * - Connects with an auth token (sent via query param `token`).
 * - Keeps StateFlows for users, rooms and messages map.
 * - Handles reconnection and triggers `sync_messages` for active rooms when reconnect succeeds.
 *
 * NOTE: event names are chosen according to project spec. Adjust if your backend uses different names.
 */
class SocketRepository(
    private val socketUrl: String = "http://10.0.2.2:3000" // change to your server URL
) {

    // Backing flows
    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val _rooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    private val _messagesByRoom = MutableStateFlow<Map<String, List<Message>>>(emptyMap())

    val users: StateFlow<List<User>> = _users
    val rooms: StateFlow<List<ChatRoom>> = _rooms
    val messagesByRoom: StateFlow<Map<String, List<Message>>> = _messagesByRoom

    private val socketScope = CoroutineScope(Dispatchers.IO)

    // Internal socket instance
    private var socket: Socket? = null

    // Track connected user id if server emits it
    var connectedUserId: String? = null

    // Simple local cache (thread-safe) used to update flows
    private val messagesCache = ConcurrentHashMap<String, MutableList<Message>>()

    fun connect(token: String) {
        if (socket?.connected() == true) return

        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            query = "token=$token"
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1000
        }

        try {
            socket = IO.socket(socketUrl, opts)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }

        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                // request initial data
                socketScope.launch {
                    emit("request_users")
                    emit("request_rooms")
                }
            }

            on("users_update") { args ->
                if (args.isNotEmpty() && args[0] is JSONArray) {
                    val arr = args[0] as JSONArray
                    val parsed = parseUsersArray(arr)
                    _users.value = parsed
                }
            }

            on("rooms_update") { args ->
                if (args.isNotEmpty() && args[0] is JSONArray) {
                    val arr = args[0] as JSONArray
                    val parsed = parseRoomsArray(arr)
                    _rooms.value = parsed
                }
            }

            on("new_message") { args ->
                if (args.isNotEmpty()) {
                    val obj = args[0]
                    if (obj is JSONObject) {
                        val message = Message.fromJson(obj)
                        appendMessage(message)
                    }
                }
            }

            on("message_history") { args ->
                // payload: { roomId: string, messages: [ ... ] }
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val payload = args[0] as JSONObject
                    val roomId = payload.optString("roomId")
                    val arr = payload.optJSONArray("messages") ?: JSONArray()
                    val list = mutableListOf<Message>()
                    for (i in 0 until arr.length()) {
                        val m = arr.optJSONObject(i) ?: continue
                        list.add(Message.fromJson(m))
                    }
                    messagesCache[roomId] = list.toMutableList()
                    _messagesByRoom.value = HashMap(messagesCache)
                }
            }

            on("user_connected") { args ->
                // server may emit current user info after auth
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val obj = args[0] as JSONObject
                    connectedUserId = obj.optString("id", null)
                }
            }

            on(Socket.EVENT_DISCONNECT) { _ ->
                // keep state; reconnection will be attempted by client
            }

            connect()
        }
    }

    fun disconnect() {
        try {
            socket?.disconnect()
            socket?.off()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket = null
        }
    }

    fun requestUsers() {
        socket?.emit("request_users")
    }

    fun requestRooms(userId: String) {
        socket?.emit("request_rooms", userId)
    }

    fun joinRoom(roomId: String) {
        socket?.emit("join_room", roomId)
    }

    fun syncMessages(roomId: String) {
        socket?.emit("sync_messages", roomId)
    }

    fun sendStopTyping(roomId: String) {
        socket?.emit("stop_typing", JSONObject().put("roomId", roomId))
    }

    fun ensurePrivateRoom(currentUserId: String, user: User): ChatRoom {
        // request server to ensure private room and return placeholder until server responds
        val id = "priv_${listOf(currentUserId, user.id).sorted().joinToString("_") }"
        val name = user.fullName.ifBlank { user.username }
        val room = ChatRoom(id = id, name = name)
        socket?.emit("ensure_private_room", JSONObject().put("userId", user.id))
        return room
    }

    fun createGroup(name: String, memberIds: List<String>, currentUserId: String): ChatRoom {
        val payload = JSONObject()
        payload.put("name", name)
        payload.put("members", JSONArray(memberIds))
        // emit with ack to receive created room
        socket?.emit("create_group", payload, Ack { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                val obj = args[0] as JSONObject
                // update rooms flow
                val room = ChatRoom(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    isGroup = true,
                    memberIds = jsonArrayToStringList(obj.optJSONArray("memberIds") ?: JSONArray()),
                    lastMessage = obj.optString("lastMessage", ""),
                    lastUpdated = obj.optLong("lastUpdated", 0L)
                )
                // append to rooms flow
                val new = _rooms.value.toMutableList()
                new.add(0, room)
                _rooms.value = new
            }
        })
        // return a placeholder room; server ack will update actual rooms flow
        return ChatRoom(id = "tmp_${System.currentTimeMillis()}", name = name, isGroup = true, memberIds = memberIds)
    }

    fun addMember(roomId: String, userId: String) {
        socket?.emit("add_member", JSONObject().put("roomId", roomId).put("userId", userId))
    }

    fun kickMember(roomId: String, userId: String) {
        socket?.emit("kick_member", JSONObject().put("roomId", roomId).put("userId", userId))
    }

    fun leaveRoom(roomId: String) {
        socket?.emit("leave_room", JSONObject().put("roomId", roomId))
    }

    fun pinRoom(roomId: String) {
        socket?.emit("pin_room", JSONObject().put("roomId", roomId))
    }

    fun muteRoom(roomId: String) {
        socket?.emit("mute_room", JSONObject().put("roomId", roomId))
    }

    fun unpinRoom(roomId: String) {
        socket?.emit("unpin_room", JSONObject().put("roomId", roomId))
    }

    fun unmuteRoom(roomId: String) {
        socket?.emit("unmute_room", JSONObject().put("roomId", roomId))
    }

    fun archiveRoom(roomId: String) {
        socket?.emit("archive_room", JSONObject().put("roomId", roomId))
    }

    fun unarchiveRoom(roomId: String) {
        socket?.emit("unarchive_room", JSONObject().put("roomId", roomId))
    }

    fun renameGroup(roomId: String, newName: String) {
        socket?.emit("rename_group", JSONObject().put("roomId", roomId).put("name", newName))
    }

    fun transferAdmin(roomId: String, newAdminId: String) {
        socket?.emit("transfer_admin", JSONObject().put("roomId", roomId).put("newAdmin", newAdminId))
    }

    fun markRoomAsRead(roomId: String) {
        socket?.emit("mark_room_read", JSONObject().put("roomId", roomId))
    }

    fun sendMessage(content: String, roomId: String, userId: String, type: String) {
        val payload = JSONObject()
        payload.put("content", content)
        payload.put("roomId", roomId)
        payload.put("senderId", userId)
        payload.put("type", type)
        // send with ack so server can confirm and return saved message object
        socket?.emit("send_message", payload, Ack { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                val obj = args[0] as JSONObject
                val message = Message.fromJson(obj)
                appendMessage(message)
            }
        })
    }

    // Helpers
    private fun parseUsersArray(arr: JSONArray): List<User> {
        val out = mutableListOf<User>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val user = User(
                id = obj.optString("id"),
                username = obj.optString("username"),
                fullName = obj.optString("fullName"),
                avatarUrl = obj.optString("avatarUrl", ""),
                phoneNumber = obj.optString("phoneNumber", "")
            )
            out.add(user)
        }
        return out
    }

    private fun parseRoomsArray(arr: JSONArray): List<ChatRoom> {
        val out = mutableListOf<ChatRoom>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val room = ChatRoom(
                id = obj.optString("id"),
                name = obj.optString("name"),
                isGroup = obj.optBoolean("isGroup", false),
                memberIds = jsonArrayToStringList(obj.optJSONArray("memberIds") ?: JSONArray()),
                lastMessage = obj.optString("lastMessage", ""),
                lastUpdated = obj.optLong("lastUpdated", 0L),
                unreadCount = obj.optInt("unreadCount", 0),
                isPinned = obj.optBoolean("isPinned", false),
                isMuted = obj.optBoolean("isMuted", false),
                isArchived = obj.optBoolean("isArchived", false)
            )
            out.add(room)
        }
        return out
    }

    private fun jsonArrayToStringList(arr: JSONArray): List<String> {
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) out.add(arr.optString(i))
        return out
    }

    private fun appendMessage(message: Message) {
        val list = messagesCache.getOrPut(message.roomId) { mutableListOf() }
        list.add(message)
        _messagesByRoom.value = HashMap(messagesCache)
    }
}
