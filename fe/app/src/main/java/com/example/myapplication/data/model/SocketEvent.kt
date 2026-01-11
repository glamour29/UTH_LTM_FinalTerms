package com.example.myapplication.data.model

object SocketEvent {
    // Connection
    const val CONNECT = "connect"
    const val DISCONNECT = "disconnect"
    const val JOIN = "join"
    
    // Messages
    const val SEND_MESSAGE = "send_message"
    const val PRIVATE_MESSAGE = "private_message"
    const val MESSAGE_RECEIVED = "message_received"
    const val MESSAGE_SEEN = "message_seen"
    const val MESSAGE_STATUS = "message_status"
    
    // Groups
    const val CREATE_GROUP = "create_group"
    const val JOIN_ROOM = "join_room"
    const val LEAVE_ROOM = "leave_room"
    
    // Typing
    const val TYPING = "typing"
    const val STOP_TYPING = "stop_typing"
    
    // Users
    const val USER_ONLINE = "user_online"
    const val USER_OFFLINE = "user_offline"
    const val GET_ONLINE_USERS = "get_online_users"
    const val ONLINE_USERS_LIST = "online_users_list"
    
    // Images
    const val SEND_IMAGE = "send_image"
    
    // Sync
    const val SYNC_MESSAGES = "sync_messages"
    
    // Block
    const val BLOCK_USER = "block_user"
    const val UNBLOCK_USER = "unblock_user"
}
