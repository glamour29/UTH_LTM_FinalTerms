// src/sockets/chatSocket.js - CLEAN ARCHITECTURE VERSION
const { ChatService } = require('../services');

// Track online users globally
const onlineUsers = new Map(); // userId -> socketId

module.exports = (io, socket) => {
    const currentUserId = socket.user.userId;
    
    // ============================================
    // 1. USER MANAGEMENT & ONLINE STATUS
    // ============================================
    
    // User joins (connect) - mark as online
    socket.on('join', async (userId) => {
        try {
            const targetUserId = userId || currentUserId;
            onlineUsers.set(targetUserId, socket.id);
            
            await ChatService.setUserOnline(targetUserId, true);
            
            const allUsers = await ChatService.getAllUsers();
            io.emit('online_users', allUsers);
            
            console.log(`[User] ${targetUserId} is now ONLINE`);
        } catch (error) {
            console.error('[Error] Join failed:', error.message);
        }
    });
    
    // Get all users
    socket.on('list_users', async () => {
        try {
            const allUsers = await ChatService.getAllUsers();
            socket.emit('online_users', allUsers);
        } catch (error) {
            console.error('[Error] Fetching users failed:', error.message);
        }
    });
    
    // Update user status
    socket.on('updateStatus', async (data) => {
        try {
            const { userId, isOnline } = data;
            await ChatService.setUserOnline(userId, isOnline);
            
            if (isOnline) {
                onlineUsers.set(userId, socket.id);
            } else {
                onlineUsers.delete(userId);
            }
            
            io.emit('user_status', { userId, online: isOnline });
            
            console.log(`[User] ${userId} status: ${isOnline ? 'ONLINE' : 'OFFLINE'}`);
        } catch (error) {
            console.error('[Error] Updating status failed:', error.message);
        }
    });
    
    // Update profile (avatar, username)
    socket.on('UpdateProfile', async (data) => {
        try {
            const { userId, username, image, fullName } = data;
            
            await ChatService.updateUserProfile(userId, {
                username,
                avatarUrl: image,
                fullName
            });
            
            const allUsers = await ChatService.getAllUsers();
            io.emit('online_users', allUsers);
            
            console.log(`[Profile] User ${userId} updated profile`);
        } catch (error) {
            console.error('[Error] Updating profile failed:', error.message);
        }
    });
    
    // ============================================
    // 2. ROOM/GROUP MANAGEMENT
    // ============================================
    
    // Join a specific room
    socket.on('join_room', (roomId) => {
        socket.join(roomId);
        console.log(`[Room] User ${currentUserId} joined room: ${roomId}`);
        socket.emit('joined_room', roomId);
    });
    
    // Get all rooms for a user
   socket.on('list_rooms', async (userId) => {
        try {
          
            const targetId = userId && userId.trim() !== "" ? userId : currentUserId;
            
            const rooms = await ChatService.getUserRooms(targetId);
            socket.emit('room_list', rooms);
        } catch (error) {
            console.error('[Error] Fetching rooms failed:', error.message);
        }
    });
    
    // Create new group
    socket.on('create_group', async (data) => {
        try {
            const { roomId, name, members } = data;
            
            const newRoom = await ChatService.createGroup(name, members, currentUserId);
            
            members.forEach(memberId => {
                const memberSocketId = onlineUsers.get(memberId);
                if (memberSocketId) {
                    io.to(memberSocketId).emit('room_updated', newRoom);
                }
            });
            
            console.log(`[Group] Created: ${name} with ${members.length} members`);
        } catch (error) {
            console.error('[Error] Creating group failed:', error.message);
            socket.emit('error', error.message);
        }
    });
    
    // Add member to group
    socket.on('add_member', async (data) => {
        try {
            const { roomId, userId } = data;
            
            await Room.findByIdAndUpdate(roomId, {
                $addToSet: { members: userId }
            });
            
            const updatedRoom = await Room.findById(roomId)
                .populate('members', 'username fullName avatarUrl isOnline');
            
            io.to(roomId).emit('room_updated', updatedRoom);
            
            console.log(`[Member] Added ${userId} to room ${roomId}`);
        } catch (error) {
            console.error('[Error] Adding member failed:', error.message);
        }
    });
    
    // Kick member from group
    socket.on('kick_member', async (data) => {
        try {
            const { roomId, userId } = data;
            
            await Room.findByIdAndUpdate(roomId, {
                $pull: { members: userId }
            });
            
            const updatedRoom = await Room.findById(roomId)
                .populate('members', 'username fullName avatarUrl isOnline');
            
            io.to(roomId).emit('room_updated', updatedRoom);
            
            console.log(`[Member] Removed ${userId} from room ${roomId}`);
        } catch (error) {
            console.error('[Error] Kicking member failed:', error.message);
        }
    });
    
    // Leave room
    socket.on('leave_room', async (data) => {
        try {
            const { roomId, userId } = data;
            socket.leave(roomId);
            
            await Room.findByIdAndUpdate(roomId, {
                $pull: { members: userId }
            });
            
            console.log(`[Room] User ${userId} left room ${roomId}`);
        } catch (error) {
            console.error('[Error] Leaving room failed:', error.message);
        }
    });
    
    // ============================================
    // 3. MESSAGING (TEXT + IMAGE)
    // ============================================
    
    // Send message (TEXT or IMAGE)
    socket.on('send_message', async (data) => {
        try {
            const { roomId, content, type, imageBase64, id } = data;
            
            const messageData = {
                roomId,
                senderId: currentUserId,
                content: type === 'IMAGE' ? imageBase64 : content,
                type: type || 'TEXT'
            };
            
            const populatedMessage = await ChatService.sendMessage(messageData);
            
            io.to(roomId).emit('receive_message', {
                id: populatedMessage._id,
                roomId: populatedMessage.roomId,
                senderId: populatedMessage.senderId._id,
                content: populatedMessage.content,
                type: populatedMessage.type,
                timestamp: populatedMessage.createdAt.getTime(),
                createdAt: populatedMessage.createdAt.toISOString()
            });
            
            console.log(`[Message] [${roomId}] ${currentUserId} sent ${type}`);
            
        } catch (error) {
            console.error('[Error] Sending message failed:', error.message);
            socket.emit('error', error.message);
        }
    });
    
    // Sync/load messages for a room
    socket.on('sync_messages', async (roomId) => {
        try {
            const messages = await ChatService.getRoomMessages(roomId, { limit: 100 });
            
            const formattedMessages = messages.map(msg => ({
                id: msg._id,
                roomId: msg.roomId,
                senderId: msg.senderId._id,
                content: msg.content,
                type: msg.type,
                timestamp: msg.createdAt.getTime(),
                createdAt: msg.createdAt.toISOString()
            }));
            
            socket.emit('load_history', formattedMessages);
            
        } catch (error) {
            console.error('[Error] Syncing messages failed:', error.message);
        }
    });
    
    // ============================================
    // 4. TYPING INDICATORS
    // ============================================
    
    socket.on('typing', (roomId) => {
        socket.to(roomId).emit('user_typing', currentUserId);
    });
    
    socket.on('stop_typing', (roomId) => {
        socket.to(roomId).emit('user_stopped_typing', currentUserId);
    });
    
    // ============================================
    // 5. MESSAGE ACTIONS (REACTIONS, PIN, DELETE)
    // ============================================
    
    socket.on('add_reaction', async (data) => {
        try {
            const { messageId, emoji } = data;
            
            const updatedMessage = await ChatService.addReactionToMessage(messageId, emoji, currentUserId);
            
            io.to(updatedMessage.roomId.toString()).emit('reaction_updated', {
                messageId,
                reactions: updatedMessage.reactions
            });
            
            console.log(`[Reaction] ${currentUserId} reacted ${emoji} to message ${messageId}`);
        } catch (error) {
            console.error('[Error] Adding reaction failed:', error.message);
        }
    });
    
    socket.on('remove_reaction', async (data) => {
        try {
            const { messageId, emoji } = data;
            
            const message = await Message.findById(messageId);
            if (message) {
                await message.removeReaction(emoji, currentUserId);
                
                const updatedMessage = await Message.findById(messageId);
                io.to(message.roomId.toString()).emit('reaction_updated', {
                    messageId,
                    reactions: updatedMessage.reactions
                });
                
                console.log(`[Reaction] ${currentUserId} removed ${emoji} from message ${messageId}`);
            }
        } catch (error) {
            console.error('[Error] Removing reaction failed:', error.message);
        }
    });
    
    socket.on('pin_message', async (data) => {
        try {
            const { messageId, roomId } = data;
            
            await Message.findByIdAndUpdate(messageId, { isPinned: true });
            
            io.to(roomId).emit('message_pinned', { messageId, isPinned: true });
            
            console.log(`[Pin] Message ${messageId} pinned in room ${roomId}`);
        } catch (error) {
            console.error('[Error] Pinning message failed:', error.message);
        }
    });
    
    socket.on('unpin_message', async (data) => {
        try {
            const { messageId, roomId } = data;
            
            await Message.findByIdAndUpdate(messageId, { isPinned: false });
            
            io.to(roomId).emit('message_pinned', { messageId, isPinned: false });
            
            console.log(`[Pin] Message ${messageId} unpinned in room ${roomId}`);
        } catch (error) {
            console.error('[Error] Unpinning message failed:', error.message);
        }
    });
    
    // Room actions (pin, mute, archive)
    socket.on('pin_room', async (data) => {
        try {
            const { roomId } = data;
            await Room.findByIdAndUpdate(roomId, { isPinned: true });
            socket.emit('room_updated', { roomId, isPinned: true });
        } catch (error) {
            console.error('[Error] Pinning room failed:', error.message);
        }
    });
    
    socket.on('mute_room', async (data) => {
        try {
            const { roomId } = data;
            await Room.findByIdAndUpdate(roomId, { isMuted: true });
            socket.emit('room_updated', { roomId, isMuted: true });
        } catch (error) {
            console.error('[Error] Muting room failed:', error.message);
        }
    });
    
    socket.on('archive_room', async (data) => {
        try {
            const { roomId } = data;
            await Room.findByIdAndUpdate(roomId, { isArchived: true });
            socket.emit('room_updated', { roomId, isArchived: true });
        } catch (error) {
            console.error('[Error] Archiving room failed:', error.message);
        }
    });
    
    // ============================================
    // 6. DISCONNECT HANDLER
    // ============================================
    
    socket.on('disconnect', async () => {
        try {
            onlineUsers.delete(currentUserId);
            await ChatService.setUserOnline(currentUserId, false);
            
            io.emit('user_status', { userId: currentUserId, online: false });
            
            console.log(`[Socket] User ${currentUserId} disconnected`);
        } catch (error) {
            console.error('[Error] Handling disconnect failed:', error.message);
        }
    });
};