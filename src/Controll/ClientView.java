package Controll; // Bạn có thể đặt nó chung với Server Control

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JLabel;

import Model.Message;
import Model.User;

public class ClientView {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1455;

    private static ClientView instance; // Singleton
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private User currentUser;
    private List<User> cachedFriendList = new ArrayList<>();

    private Consumer<Packet> onPacketReceived;
    
    private AudioCallHandler audioCallHandler;
    private VideoCallHandler videoCallHandler;
    private JLabel currentVideoLabel;
    
    private ClientView() {}

    // Singleton pattern
    public static synchronized ClientView getInstance() {
        if (instance == null) {
            instance = new ClientView();
        }
        return instance;
    }

    public boolean connect() {
        try {
            if (socket != null && !socket.isClosed()) {
                return true; // Đã kết nối
            }
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("đanggg nối tới server thành công!");

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            startListening();
            
            System.out.println("Kết nối tới server thành công!");
            return true;
        } catch (IOException e) {
            System.err.println("Không thể kết nối tới server: " + e.getMessage());
            return false;
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (socket.isConnected()) {
                    Packet packet = (Packet) in.readObject();
                    
                    // Gửi Packet này về cho MainFrame xử lý
                    if (onPacketReceived != null) {
                        onPacketReceived.accept(packet);
                    }
                }
            } catch (Exception e) {
                System.err.println("Mất kết nối tới server.");
                // (Xử lý mất kết nối ở đây)
            }
        }).start();
    }
    
    /**
     * Gửi một Packet (đã đóng gói) lên Server
     */
//    public void sendPacket(Packet packet) {
//        if (out == null) {
//            System.err.println("Chưa kết nối, không thể gửi packet!");
//            return;
//        }
//        try {
//            // Dùng synchronized để đảm bảo không bị lỗi khi nhiều luồng cùng gửi
//            synchronized (out) {
//                out.writeObject(packet);
//                out.flush();
//                out.reset(); // Quan trọng
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    
    public synchronized void sendPacket(Packet packet) {
        if (socket == null || socket.isClosed() || out == null) {
            System.err.println("[ClientView] Socket chưa sẵn sàng, không thể gửi packet!");
            return;
        }

        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("[ClientView] Lỗi khi gửi packet: " + e.getMessage());
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.err.println("[ClientView] Kết nối đã đóng.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    
    // --- Các hàm tiện ích ---
    
    /**
     * Gửi yêu cầu đăng nhập
     */
    public void login(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);

        System.out.println("[CLIENT] Sending login packet with user = " + user.getUsername());
        sendPacket(new Packet(MessageType.LOGIN_REQUEST, user));    }
    
    public void register(String username, String password, String phoneNumber) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // Dùng User object để gửi thông tin
        user.setPhoneNumber(phoneNumber);
        sendPacket(new Packet(MessageType.REGISTER_REQUEST, user));
    }
    
    /**
     * Gửi yêu cầu lấy danh sách bạn bè
     */
    public void requestFriendList() {
        sendPacket(new Packet(MessageType.FRIEND_LIST_REQUEST, null));
    }
    
    public void acceptFriendRequest(int requesterId) {
        sendPacket(new Packet(MessageType.FRIEND_REQUEST_ACCEPT, requesterId));
    }

    
    public void sendFriendRequest(String phoneNumber) {
        // Gửi 1 Packet chứa số điện thoại người muốn kết bạn
        sendPacket(new Packet(MessageType.FRIEND_REQUEST_SEND, phoneNumber));
    }

    public void searchUserByPhoneNumber(String phoneNumber) {
        sendPacket(new Packet(MessageType.USER_SEARCH_REQUEST, phoneNumber));
    }
    public void requestPendingFriendRequests() {
        sendPacket(new Packet(MessageType.REQUEST_PENDING_FRIENDS, null));
    }
    
    public void createGroup(String groupName, List<Integer> memberIds) {
        // Tạo payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("groupName", groupName);
        payload.put("memberIds", memberIds);
        
        // Gửi packet
        sendPacket(new Packet(MessageType.GROUP_CREATE_REQUEST, payload));
    }

    /**
     * Gửi một tin nhắn
     */
    public void sendMessage(int conversationId, String content) {
        Message msg = new Message();
        msg.setConversationId(conversationId); 
        msg.setContent(content);
        sendPacket(new Packet(MessageType.MESSAGE_SEND, msg));
    }
    
    /**
     * Gửi ảnh
     */
    public void sendImageMessage(int conversationId, String base64Image, String fileName) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setContent("[IMAGE]"); // Placeholder
        msg.setHasImage(true);
        msg.setImageBase64(base64Image); // Base64 để gửi qua socket
        msg.setImageFileName(fileName);
        
        sendPacket(new Packet(MessageType.MESSAGE_SEND, msg));
        System.out.println("[ClientView] Sending image message: " + fileName);
    }
    
    public void requestHistory(int conversationId) {
        sendPacket(new Packet(MessageType.HISTORY_REQUEST, conversationId));
        System.out.println("[ClientView] Requested chat history for convo ID: " + conversationId);
    }
    
    /**
     * Gửi file kèm text mô tả
     */
    public void sendFileMessage(int conversationId, String base64File, String fileName) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setHasImage(true);
        msg.setImageBase64(base64File);
        msg.setImageFileName(fileName);

        sendPacket(new Packet(MessageType.MESSAGE_SEND, msg));
        System.out.println("[ClientView] Sending file with description: " + fileName );
    }
    public void requestConversationList() {
    	sendPacket(new Packet(MessageType.CONVERSATION_LIST_REQUEST, null));
    	System.out.println("[ClientView] Requesting conversation list...");
    }
    
    // --- Getters & Setters ---
    
    public void setOnPacketReceived(Consumer<Packet> handler) {
        this.onPacketReceived = handler;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
    public List<User> getFriendList() {
        return cachedFriendList;
    }
    public void setFriendList(List<User> friends) {
        this.cachedFriendList = friends != null ? friends : new ArrayList<>();
    }
    
    
//    public void initiateCall(int friendId) {
//        // Gửi packet TCP lên Server báo: "Tôi muốn gọi cho friendId"
//        // Server sẽ tìm IP của friendId và gửi packet CALL_REQUEST cho người đó
//    	Packet requestPacket = new Packet(MessageType.CALL_REQUEST, null);
//        requestPacket.setReceiverId(friendId); // <--- QUAN TRỌNG NHẤT
//        
//        sendPacket(requestPacket);
//        
//        System.out.println("[ClientView] Đang gọi cho ID: " + friendId);
//    }
    public void initiateCall(int friendId, String callType) {
        // 1. Luôn khởi tạo Audio trước để lấy cổng Audio
        audioCallHandler = new AudioCallHandler();
        int myAudioPort = audioCallHandler.getLocalPort();
        
        String myPayload = "";
        MessageType msgType;

        if (callType.equals("VIDEO")) {
            // 2. Nếu là Video, khởi tạo thêm VideoHandler để lấy cổng Video
            // (Truyền null vào VideoHandler vì chưa có khung hình để hiển thị, MainFrame sẽ set sau)
            videoCallHandler = new VideoCallHandler(null); 
            int myVideoPort = videoCallHandler.getLocalPort();
            
            // Payload dạng: "AUDIO_PORT:VIDEO_PORT"
            myPayload = myAudioPort + ":" + myVideoPort;
            msgType = MessageType.VIDEO_CALL_REQUEST; // Bạn cần thêm cái này vào Enum MessageType
            
            System.out.println("[ClientView] Gọi Video... AudioPort=" + myAudioPort + ", VideoPort=" + myVideoPort);
        } else {
            // Chỉ Audio
            myPayload = String.valueOf(myAudioPort);
            msgType = MessageType.CALL_REQUEST; // Audio Call
            
            System.out.println("[ClientView] Gọi Audio... Port=" + myAudioPort);
        }
        
        Packet requestPacket = new Packet(msgType, myPayload);
        requestPacket.setReceiverId(friendId);
        sendPacket(requestPacket);
    }
    
//    public void acceptCall(int callerId, String callerIP) {
//    	Packet packet = new Packet(MessageType.CALL_ACCEPT, null);
//        packet.setReceiverId(callerId); // <--- QUAN TRỌNG: Gửi trả lời cho người gọi (callerId)
//        sendPacket(packet);
//        
//        // Bắt đầu bắn UDP
//        startVoiceChat(callerIP);
//    }
 // Trong Controll/ClientView.java

    public void acceptCall(int callerId, String callerPayload) {
        // ... (Phần xử lý khởi tạo UDP để nghe - code cũ giữ nguyên) ...
        
        // 1. Parse thông tin người gọi để biết bắn tin đi đâu
        String[] parts = callerPayload.split(":");
        String targetIP = parts[0];
        int targetPort = Integer.parseInt(parts[1]);
        
        // 2. Khởi tạo Audio của mình
        audioCallHandler = new AudioCallHandler();
        int myPort = audioCallHandler.getLocalPort(); // Ví dụ: 50091
        
        // Set đích đến là người gọi
        audioCallHandler.setTarget(targetIP, targetPort);
        audioCallHandler.startCall();
        
        // Chuyển số cổng thành chuỗi (Ví dụ: "50091")
        String myPayload = String.valueOf(myPort); 
        
        // Tạo gói tin ACCEPT, nhét số cổng vào
        Packet packet = new Packet(MessageType.CALL_ACCEPT, myPayload);
        packet.setReceiverId(callerId);
        
        // Gửi đi
        sendPacket(packet);
        
        System.out.println("[ClientView] Đã chấp nhận cuộc gọi. Cổng của tôi là: " + myPayload);
    }
    public void rejectCall(int callerId) {
        sendPacket(new Packet(MessageType.CALL_DENY, callerId));
    }
    
    public void endCall(int partnerId) {
        // Dừng luồng UDP
        if (audioCallHandler != null) {
            audioCallHandler.stopCall();
            audioCallHandler = null;
        }
        // Gửi báo hiệu cho bên kia biết
        if (partnerId > 0) {
            Packet packet = new Packet(MessageType.CALL_END, null);
            packet.setReceiverId(partnerId); // <--- QUAN TRỌNG
            sendPacket(packet);
        }    }
    
    public void startVoiceChat(String targetIP, int targetPort) {
        // 1. Tạo mới
        audioCallHandler = new AudioCallHandler();
        
        // 2. Set đích TRƯỚC
        audioCallHandler.setTarget(targetIP, targetPort);
        
        // 3. Rồi mới Start
        audioCallHandler.startCall(); 
    }
    
    public void startVideoChat(String targetIP, int targetAudioPort, int targetVideoPort, javax.swing.JLabel videoScreen) {
        // 1. Setup Audio (Nếu chưa có thì new, nếu có rồi thì setTarget)
        if (audioCallHandler == null) audioCallHandler = new AudioCallHandler();
        audioCallHandler.setTarget(targetIP, targetAudioPort);
        audioCallHandler.startCall();

        // 2. Setup Video
        if (videoCallHandler == null) videoCallHandler = new VideoCallHandler(videoScreen);
        else videoCallHandler.setDisplayLabel(videoScreen); // Cần thêm hàm này trong VideoCallHandler
        JLabel labelToUse = (videoScreen != null) ? videoScreen : currentVideoLabel;
        videoCallHandler = new VideoCallHandler(labelToUse);
        
        videoCallHandler.setTarget(targetIP, targetVideoPort);
        videoCallHandler.startCall();
    }
    
    public void setRemoteVideoLabel(JLabel label) {
        this.currentVideoLabel = label;
        // Nếu đang có video handler chạy rồi thì update label cho nó luôn
        if (videoCallHandler != null) {
            videoCallHandler.setDisplayLabel(label);
        }
    }


    public void onVideoCallAccepted(String partnerPayload, javax.swing.JLabel videoScreen) {
        // 1. Parse thông tin: IP, AudioPort, VideoPort
        String[] parts = partnerPayload.split(":");
        String targetIP = parts[0];
        int targetAudioPort = Integer.parseInt(parts[1]);
        int targetVideoPort = Integer.parseInt(parts[2]);
        
        System.out.println("[ClientView] Bắt đầu nhận hình từ: " + targetIP + ":" + targetVideoPort);

        // 2. Kích hoạt Audio (như cũ)
        if (audioCallHandler != null) {
            audioCallHandler.setTarget(targetIP, targetAudioPort);
            audioCallHandler.startCall();
        }

        // 3. Kích hoạt Video (QUAN TRỌNG)
        if (videoCallHandler != null) {
            // Cập nhật lại màn hình hiển thị (Cái JLabel đen thui lúc nãy)
            videoCallHandler.setDisplayLabel(videoScreen);
            
            // Set địa chỉ để gửi hình mình đi
            videoCallHandler.setTarget(targetIP, targetVideoPort);
            
            // Bắt đầu chạy: Thu hình mình gửi đi & Nhận hình họ hiện lên Label
            videoCallHandler.startCall();
        }
    }
 // Trong ClientView.java

    /**
     * HÀM MỚI: Chấp nhận cuộc gọi VIDEO
     */
    public void acceptVideoCall(int callerId, String callerPayload) {
        // 1. Parse thông tin người gọi (IP:AudioPort:VideoPort)
        String[] parts = callerPayload.split(":");
        String targetIP = parts[0];
        int targetAudioPort = Integer.parseInt(parts[1]);
        int targetVideoPort = Integer.parseInt(parts[2]);

        System.out.println("[ClientView] Chấp nhận Video Call từ: " + targetIP);

        // 2. Khởi tạo Audio của mình
        audioCallHandler = new AudioCallHandler();
        int myAudioPort = audioCallHandler.getLocalPort();
        audioCallHandler.setTarget(targetIP, targetAudioPort);
        audioCallHandler.startCall();

        // 3. Khởi tạo Video của mình
        // (Lưu ý: MainFrame phải setRemoteVideoLabel trước khi gọi hàm này)
        if (videoCallHandler == null) {
            videoCallHandler = new VideoCallHandler(currentVideoLabel); 
        } else {
            videoCallHandler.setDisplayLabel(currentVideoLabel);
        }
        
        int myVideoPort = videoCallHandler.getLocalPort();
        videoCallHandler.setTarget(targetIP, targetVideoPort);
        videoCallHandler.startCall();

        // 4. QUAN TRỌNG: Đóng gói cả 2 Port để gửi lại cho người gọi
        String myPayload = myAudioPort + ":" + myVideoPort;
        
        Packet packet = new Packet(MessageType.CALL_ACCEPT, myPayload);
        packet.setReceiverId(callerId);
        sendPacket(packet);
    }
    

    /**
     * Hàm xử lý khi nhận được tin CALL_ACCEPT (Audio)
     * payload có dạng: "192.168.1.5:50001"
     */
    public void onCallAccepted(String payload) {
        try {
            // 1. Cắt chuỗi dựa trên dấu hai chấm
            String[] parts = payload.split(":");
            
            if (parts.length == 2) {
                String targetIP = parts[0];             // "192.168.1.5"
                int targetPort = Integer.parseInt(parts[1]); // 50001
                
                System.out.println("[ClientView] Audio kết nối tới: " + targetIP + ":" + targetPort);

                // 2. Khởi tạo Audio Handler (nếu chưa có)
                if (audioCallHandler == null) {
                    audioCallHandler = new AudioCallHandler();
                }
                
                // 3. Set đích đến và Bắt đầu
                // (Hàm setTarget này nằm trong AudioCallHandler.java)
                audioCallHandler.setTarget(targetIP, targetPort);
                audioCallHandler.startCall();
                
            } else {
                System.err.println("[ClientView] Payload lỗi (không đủ IP:Port): " + payload);
            }
        } catch (Exception e) {
            System.err.println("[ClientView] Lỗi parse payload Audio: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}