package Controll;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private int port;
    private static final int PORT = 1455;

    private Map<Integer, Client> onlineUsers = new ConcurrentHashMap<>();

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket);

                // Dùng ObjectInputStream/ObjectOutputStream
                Client handler = new Client(socket, this); 
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===== Quản lý user online (đã đổi sang ID) =====
    public void addOnlineUser(int userId, Client handler) {
        onlineUsers.put(userId, handler);
        System.out.println("User " + userId + " added. Total online: " + onlineUsers.size());
        notifyFriendStatusChange(userId, true); // Thông báo cho bạn bè
    }

    public void removeOnlineUser(int userId) {
        onlineUsers.remove(userId);
        System.out.println("User " + userId + " removed. Total online: " + onlineUsers.size());
        notifyFriendStatusChange(userId, false); // Thông báo cho bạn bè
    }
    private void notifyFriendStatusChange(int userId, boolean isOnline) {
        MessageType type = isOnline ? MessageType.USER_ONLINE_NOTICE : MessageType.USER_OFFLINE_NOTICE;
        Packet statusPacket = new Packet(type, userId); // Gửi ID của người vừa online/offline
        
        // for (User friend : friends) {
        //     sendPacket(friend.getId(), statusPacket);
        // }
        
        // Tạm thời, chúng ta broadcast cho mọi người
        System.out.println("Broadcasting status change for user " + userId);
        for (Integer friendId : onlineUsers.keySet()) {
            if (friendId != userId) {
                sendPacket(friendId, statusPacket);
            }
        }
    }

    public Client getHandlerByUserId(int userId) {
        return onlineUsers.get(userId);
    }
    
    
    public void sendPacket(int targetUserId, Packet packet) {
    	Client handler = onlineUsers.get(targetUserId);
        if (handler != null) {
            handler.sendPacket(packet);
        } else {
            System.out.println("User " + targetUserId + " is offline. Packet not sent.");
        }
    }
    

    public static void main(String[] args) {
    	Server server = new Server(PORT);
        server.start();
    }
}