package Controll;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import DataAccess.CloudinaryUtils;
import DataAccess.ConversationDAO;
import DataAccess.ConversationMemberDAO;
import DataAccess.FriendshipDAO;
import DataAccess.MessageDAO;
import DataAccess.UserDAO;
import Model.Conversation;
import Model.ConversationType;
import Model.FriendSearchResult;
import Model.Message;
import Model.User;

public class Client implements Runnable {
    private Socket socket;
    private Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private User currentUser;
    
    private UserDAO userDAO = new UserDAO();
    private FriendshipDAO friendshipDAO = new FriendshipDAO();
    private ConversationDAO conversationDAO = new ConversationDAO();
    private MessageDAO messageDAO = new MessageDAO();
    private ConversationMemberDAO memberDAO = new ConversationMemberDAO();
    
    public Client(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Packet request = (Packet) in.readObject();
                MessageType type = request.getType();

                // --- PHẦN XỬ LÝ ĐỊNH TUYẾN CUỘC GỌI (VOICE/VIDEO CALL) ---
                if (type == MessageType.CALL_REQUEST || 
                    type == MessageType.CALL_ACCEPT || 
                    type == MessageType.CALL_DENY || 
                    type == MessageType.CALL_END ||
                    type == MessageType.VIDEO_CALL_REQUEST) { // Thêm VIDEO_CALL_REQUEST nếu có
                    
                    // 1. Xác định người nhận
                    int targetId = request.getReceiverId();
                    
                    // 2. Đóng dấu người gửi & Xử lý IP
                    if (currentUser != null) {
                        request.setSenderId(currentUser.getId());
                        
                        // Xử lý ghép IP vào Payload cho các loại gói tin cần thiết
                        if (type == MessageType.CALL_REQUEST || 
                            type == MessageType.VIDEO_CALL_REQUEST || 
                            type == MessageType.CALL_ACCEPT) {
                            
                            request.setSenderName(currentUser.getUsername());
                            
                            // Lấy IP thật từ Socket kết nối
                            String realIP = socket.getInetAddress().getHostAddress();
                            
                            // Payload hiện tại là Port do Client gửi lên (ví dụ "54321" hoặc "5000:6000")
                            String clientPortPayload = (String) request.getPayload();
                            
                            // Ghép thành "IP:PORT" (ví dụ "192.168.1.5:54321")
                            String fullAddress = realIP + ":" + clientPortPayload;
                            
                            // Cập nhật lại payload trước khi chuyển đi
                            request.setPayload(fullAddress);
                        }
                    }

                    System.out.println("[SERVER] Routing CALL packet: " + type + 
                                       " From: " + (currentUser != null ? currentUser.getUsername() : "Unknown") + 
                                       " To UserID: " + targetId + 
                                       " Payload: " + request.getPayload());

                    // 3. Chuyển tiếp ngay lập tức
                    server.sendPacket(targetId, request);
                    
                    // 4. Bỏ qua các xử lý khác
                    continue; 
                }
                
                handlePacket(request);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client " + (currentUser != null ? currentUser.getUsername() : "unknown") + " disconnected.");
        } finally {
            cleanup();
        }
    }

    private void handlePacket(Packet request) {
        System.out.println("[SERVER] Received packet type = " + request.getType() 
            + ", payload = " + request.getPayload());
        
        MessageType type = request.getType();
        
        // ✅ Nếu chưa login, chỉ chấp nhận LOGIN/REGISTER
        if (currentUser == null) {
            if (type == MessageType.LOGIN_REQUEST) {
                handleLogin((User) request.getPayload());
            } else if (type == MessageType.REGISTER_REQUEST) {
                handleRegister((User) request.getPayload());
            }
            return;
        }
        
        // ✅ Đã login, xử lý các request khác
        switch (type) {
            case FRIEND_LIST_REQUEST:
                handleFriendListRequest();
                break;
                
            case FRIEND_REQUEST_SEND:
                handleFriendRequest((String) request.getPayload());
                break;
                
            case FRIEND_REQUEST_ACCEPT:
                handleFriendAccept((Integer) request.getPayload());
                break;

            case USER_SEARCH_REQUEST:
                handleUserSearch((String) request.getPayload());
                break;
                
            case REQUEST_PENDING_FRIENDS:
                handlePendingFriendRequests();
                break;
                
            case MESSAGE_SEND:
                handleMessageSend((Message) request.getPayload());
                break;
                
            case HISTORY_REQUEST:
                handleHistoryRequest((Integer) request.getPayload());
                break;
                
            case GROUP_CREATE_REQUEST:
                handleGroupCreateRequest((Map<String, Object>) request.getPayload());
                break;
                
            case CONVERSATION_LIST_REQUEST:
                handleConversationListRequest();
                break;
                
            case LOGOUT_REQUEST:
                cleanup();
                break;
                
            default:
                System.err.println("[SERVER] Unknown packet type: " + type);
                break;
        }
    }

    // ============================================================
    // =================== XỬ LÝ ĐĂNG KÝ/ĐĂNG NHẬP =================
    // ============================================================

    private void handleRegister(User registerAttempt) {
        try {
            String username = registerAttempt.getUsername();
            String password = registerAttempt.getPassword();
            String phoneNumber = registerAttempt.getPhoneNumber();

            User existingUser = userDAO.findUserByUsername(username);
            
            if (existingUser != null) {
                sendPacket(new Packet(MessageType.REGISTER_FAILURE, "Tên đăng nhập '" + username + "' đã được sử dụng."));
                return;
            }

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                sendPacket(new Packet(MessageType.REGISTER_FAILURE, "Số điện thoại không được để trống."));
                return;
            }

            if (userDAO.isPhoneNumberExists(phoneNumber)) {
                sendPacket(new Packet(MessageType.REGISTER_FAILURE, "Số điện thoại '" + phoneNumber + "' đã được sử dụng."));
                return;
            }

            // TODO: Hash password trước khi lưu (BCrypt, Argon2, etc.)
            String hashedPassword = password; // Tạm thời dùng plaintext

            User newUser = userDAO.createUser(username, phoneNumber, hashedPassword);

            if (newUser != null) {
                System.out.println("[SERVER] New user registered: " + username);
                sendPacket(new Packet(MessageType.REGISTER_SUCCESS, "Đăng ký thành công! Giờ bạn có thể đăng nhập."));
            } else {
                sendPacket(new Packet(MessageType.REGISTER_FAILURE, "Không thể tạo tài khoản. Vui lòng thử lại."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendPacket(new Packet(MessageType.REGISTER_FAILURE, "Lỗi server khi đăng ký."));
        }
    }

    private void handleLogin(User loginAttempt) {
        try {
            if (loginAttempt == null) {
                System.err.println("[SERVER] LOGIN_REQUEST payload null");
                sendPacket(new Packet(MessageType.LOGIN_FAILURE, "Dữ liệu đăng nhập không hợp lệ."));
                return;
            }

            User userFromDB = userDAO.findUserByUsername(loginAttempt.getUsername());
            
            if (userFromDB != null && userFromDB.getPassword().equals(loginAttempt.getPassword())) {
                // Kiểm tra đã login ở nơi khác chưa
                if (server.getHandlerByUserId(userFromDB.getId()) != null) {
                    sendPacket(new Packet(MessageType.LOGIN_FAILURE, "Tài khoản đang đăng nhập ở nơi khác."));
                    return;
                }

                this.currentUser = userFromDB;
                userDAO.setUserOnline(currentUser.getId(), true);
                server.addOnlineUser(currentUser.getId(), this);
                
                System.out.println("[SERVER] User logged in: " + currentUser.getUsername());
                sendPacket(new Packet(MessageType.LOGIN_SUCCESS, userFromDB));
                
            } else {
                sendPacket(new Packet(MessageType.LOGIN_FAILURE, "Sai tên đăng nhập hoặc mật khẩu."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendPacket(new Packet(MessageType.LOGIN_FAILURE, "Lỗi server khi đăng nhập."));
        }
    }

    // ============================================================
    // =================== XỬ LÝ KẾT BẠN ==========================
    // ============================================================

    private void handleFriendRequest(String phoneNumber) {
        try {
            User userByPhone = userDAO.findUserByPhoneNumber(phoneNumber);
            
            if (userByPhone == null) {
                sendPacket(new Packet(MessageType.FRIEND_REQUEST_FAILURE, 
                    "Người dùng số điện thoại '" + phoneNumber + "' không tồn tại."));
                return;
            }

            if (userByPhone.getId() == currentUser.getId()) {
                sendPacket(new Packet(MessageType.FRIEND_REQUEST_FAILURE, 
                    "Bạn không thể tự kết bạn với chính mình!"));
                return;
            }

            if (friendshipDAO.areFriendsOrPending(currentUser.getId(), userByPhone.getId())) {
                sendPacket(new Packet(MessageType.FRIEND_REQUEST_FAILURE, 
                    "Hai người đã là bạn hoặc đang chờ xác nhận."));
                return;
            }

            friendshipDAO.createFriendRequest(currentUser.getId(), userByPhone.getId());
            
            System.out.println("[SERVER] Friend request: " + currentUser.getUsername() 
                + " -> " + userByPhone.getUsername());
            
            sendPacket(new Packet(MessageType.FRIEND_REQUEST_SUCCESS, 
                "Đã gửi lời mời kết bạn tới " + phoneNumber));

            // ✅ Gửi thông báo real-time cho người được mời (nếu đang online)
            Client targetClient = server.getHandlerByUserId(userByPhone.getId());
            if (targetClient != null) {
                targetClient.sendPacket(new Packet(MessageType.FRIEND_REQUEST_RECEIVED, currentUser));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendPacket(new Packet(MessageType.FRIEND_REQUEST_FAILURE, "Lỗi khi gửi yêu cầu."));
        }
    }

    private void handleUserSearch(String phoneNumber) {
        FriendSearchResult result = new FriendSearchResult();
        result.setQuery(phoneNumber);

        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                result.setStatus(FriendSearchResult.Status.NOT_FOUND);
                sendPacket(new Packet(MessageType.USER_SEARCH_RESPONSE, result));
                return;
            }

            User userByPhone = userDAO.findUserByPhoneNumber(phoneNumber.trim());

            if (userByPhone == null) {
                result.setStatus(FriendSearchResult.Status.NOT_FOUND);
            } else if (userByPhone.getId() == currentUser.getId()) {
                result.setStatus(FriendSearchResult.Status.SELF);
            } else if (friendshipDAO.areFriendsOrPending(currentUser.getId(), userByPhone.getId())) {
                result.setStatus(FriendSearchResult.Status.ALREADY_CONNECTED);
            } else {
                result.setStatus(FriendSearchResult.Status.FOUND);
                userByPhone.setPassword(null); // Không gửi password về client
                result.setUser(userByPhone);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.setStatus(FriendSearchResult.Status.NOT_FOUND);
        }

        sendPacket(new Packet(MessageType.USER_SEARCH_RESPONSE, result));
    }

    private void handleFriendAccept(int requesterId) {
        try {
            // Chấp nhận lời mời từ requesterId
            friendshipDAO.acceptFriendRequest(requesterId, currentUser.getId());
            
            System.out.println("[SERVER] Friend accepted: " + currentUser.getUsername() 
                + " accepted request from user ID " + requesterId);
            
            sendPacket(new Packet(MessageType.FRIEND_REQUEST_SUCCESS, 
                "Đã chấp nhận lời mời kết bạn."));
            
            // ✅ Gửi thông báo cho người gửi lời mời ban đầu
            Client requesterClient = server.getHandlerByUserId(requesterId);
            if (requesterClient != null) {
                requesterClient.sendPacket(new Packet(MessageType.FRIEND_REQUEST_ACCEPTED, currentUser));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendPacket(new Packet(MessageType.FRIEND_REQUEST_FAILURE, 
                "Lỗi khi chấp nhận lời mời."));
        }
    }

    private void handlePendingFriendRequests() {
        try {
            // ✅ Lấy danh sách những người đã GỬI lời mời cho currentUser
            List<User> pendingRequests = friendshipDAO.getPendingFriendRequests(currentUser.getId());
            
            System.out.println("[SERVER] handlePendingFriendRequests: Found " + pendingRequests.size() 
                + " pending requests for user " + currentUser.getUsername());
            
            // Debug: Kiểm tra type của objects trong list
            if (!pendingRequests.isEmpty()) {
                System.out.println("[SERVER] First item type: " + pendingRequests.get(0).getClass().getName());
            }
            
            sendPacket(new Packet(MessageType.PENDING_FRIEND_REQUESTS_RESPONSE, pendingRequests));
            System.out.println("[SERVER] Sent PENDING_FRIEND_REQUESTS_RESPONSE with " + pendingRequests.size() + " items");
            
        } catch (Exception e) {
            System.err.println("[SERVER] Error in handlePendingFriendRequests: " + e.getMessage());
            e.printStackTrace();
            sendPacket(new Packet(MessageType.PENDING_FRIEND_REQUESTS_RESPONSE, Collections.emptyList()));
        }
    }

    private void handleFriendListRequest() {
        try {
            List<User> friends = friendshipDAO.getFriendsByUserId(currentUser.getId());
            
            for (User friend : friends) {
                // Kiểm tra xem server có đang giữ kết nối của friend này không
                if (server.getHandlerByUserId(friend.getId()) != null) {
                    friend.setOnline(true); // ✅ Đánh dấu là đang Online
                } else {
                    friend.setOnline(false);
                }
            }
            System.out.println("[SERVER] Sending " + friends.size() 
                + " friends to " + currentUser.getUsername());
            
            sendPacket(new Packet(MessageType.FRIEND_LIST_RESPONSE, friends));
        } catch (Exception e) {
            e.printStackTrace();
            sendPacket(new Packet(MessageType.FRIEND_LIST_RESPONSE, Collections.emptyList()));
        }
    }

    // ============================================================
    // =================== XỬ LÝ TIN NHẮN ==========================
    // ============================================================
    
    private void handleMessageSend(Message message) {
        try {
            if (message == null) {
                System.err.println("[SERVER] handleMessageSend: message is null");
                return;
            }
            
            int conversationId = message.getConversationId();
            if (conversationId <= 0) {
                System.err.println("[SERVER] Invalid conversationId: " + conversationId);
                return;
            }
            
            // ========== XỬ LÝ ẢNH/FILE NẾU CÓ ==========
            if (message.hasImage() && message.getImageBase64() != null) {
                try {
                    String base64Data = message.getImageBase64();
                    String fileName = message.getImageFileName();
                    
                    if (fileName == null || fileName.isEmpty()) {
                        fileName = "file_" + System.currentTimeMillis() + ".dat";
                    }
                    
                    // Decode Base64
                    byte[] fileBytes = Base64.getDecoder().decode(base64Data);
                    
                    System.out.println("[SERVER] Uploading file to Cloudinary: " + fileName 
                        + " (" + fileBytes.length + " bytes)");
                    
                    // Kiểm tra xem là image hay file thông thường
                    String uploadedUrl;
                    if (CloudinaryUtils.isImageFile(fileName)) {
                        // Upload như image (có thể xem trực tiếp)
                        uploadedUrl = CloudinaryUtils.uploadImageFromBytes(fileBytes, fileName);
                        System.out.println("[SERVER] Image uploaded successfully");
                    } else {
                        // Upload như file thông thường (PDF, DOCX, TXT, etc.)
                        uploadedUrl = CloudinaryUtils.uploadFileFromBytes(fileBytes, fileName);
                        System.out.println("[SERVER] File uploaded successfully");
                    }
                    
                    // Cập nhật message với URL từ Cloudinary
                    message.setImageUrl(uploadedUrl);  // Dùng imageUrl cho cả file
                    message.setImageFileName(fileName);
                    message.setImageBase64(null); // Xóa Base64 để không lưu vào DB
                    
                    // Set content phù hợp
                    if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                        if (CloudinaryUtils.isImageFile(fileName)) {
                            message.setContent("[Đã gửi ảnh]");
                        } else {
                            message.setContent("[Đã gửi file: " + fileName + "]");
                        }
                    }
                    
                    System.out.println("[SERVER] Upload successful: " + uploadedUrl);
                    
                } catch (Exception e) {
                    System.err.println("[SERVER] Error uploading to Cloudinary: " + e.getMessage());
                    e.printStackTrace();
                    // Vẫn tiếp tục xử lý message bình thường (không có file)
                    message.setHasImage(false);
                    message.setImageBase64(null);
                }
            }
            
            
            // ========== XỬ LÝ MESSAGE BÌNH THƯỜNG ==========
            // 1. Tìm hoặc tạo conversation
//            Conversation convo = findOrCreateDirectConversation(conversationId);
//            
//            if (convo == null) {
//                System.err.println("[SERVER] Failed to create conversation!");
//                return;
//            }
            
//            System.out.println("[SERVER] Using conversation ID: " + convo.getId());
            
            // 2. Hoàn thiện object Message
            message.setConversationId(conversationId);
            message.setSenderId(currentUser.getId());
            message.setSenderName(currentUser.getUsername());

            // 3. Lưu vào CSDL
            Message savedMessage = messageDAO.createMessage(message);
            
            if (savedMessage != null) {
            	  if (savedMessage.getSenderName() == null) {
                      savedMessage.setSenderName(currentUser.getUsername());
                  }
                System.out.println("[SERVER] Message saved: " + currentUser.getUsername() 
                    + " -> user ID " + conversationId);
                	
                List<Integer> memberIds = memberDAO.getMemberIds(conversationId);
                System.out.println("[SERVER] Broadcasting message to " + memberIds.size() + " members.");
                Packet messagePacket = new Packet(MessageType.MESSAGE_RECEIVE, savedMessage);
                
//                if (savedMessage.hasImage()) {
//                    System.out.println("[SERVER] File/Image URL: " + savedMessage.getImageUrl());
//                    System.out.println("[SERVER] Filename: " + savedMessage.getImageFileName());
//                }
//                
//                // 4. Gửi cho người nhận (nếu họ online)
//                Client friendClient = server.getHandlerByUserId(conversationId);
//                if (friendClient != null) {
//                    friendClient.sendPacket(new Packet(MessageType.MESSAGE_RECEIVE, savedMessage));
//                    System.out.println("[SERVER] Message delivered to online user");
//                } else {
//                    System.out.println("[SERVER] Recipient is offline, message saved to DB");
//                }
//            } else {
//                System.err.println("[SERVER] Failed to save message to database");
//            }
                for (int memberId : memberIds) {
                    // Không gửi lại cho chính người gửi
                    if (memberId == currentUser.getId()) {
                        continue; 
                    }
                    
                    Client memberClient = server.getHandlerByUserId(memberId);
                    if (memberClient != null) {
                        // Gửi cho từng thành viên đang online
                        memberClient.sendPacket(messagePacket);
                    } else {
                        System.out.println("[SERVER] Member " + memberId + " is offline, message saved to DB");
                    }
                }
            } else {
                System.err.println("[SERVER] Failed to save message to database");
            }
            
        } catch (Exception e) {
            System.err.println("[SERVER] Error in handleMessageSend: " + e.getMessage());
            e.printStackTrace();
        }
    }

//    private void handleMessageSend(Message message) {
//        try {
//            if (message == null) {
//                System.err.println("[SERVER] handleMessageSend: message is null");
//                return;
//            }
//            
//            int friendId = message.getReceiverId();
//            System.out.println("[SERVER] Processing message from " + currentUser.getUsername() 
//                + " to user ID " + friendId);
//            
//            // ========== XỬ LÝ ẢNH NẾU CÓ ==========
//            if (message.hasImage() && message.getImageBase64() != null) {
//                try {
//                    String base64Data = message.getImageBase64();
//                    String fileName = message.getImageFileName();
//                    
//                    if (fileName == null || fileName.isEmpty()) {
//                        fileName = "image_" + System.currentTimeMillis() + ".jpg";
//                    }
//                    
//                    // Decode Base64
//                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
//                    
//                    System.out.println("[SERVER] Uploading image to Cloudinary: " + fileName + " (" + imageBytes.length + " bytes)");
//                    
//                    // Upload lên Cloudinary
//                    String imageUrl = CloudinaryUtils.uploadImageFromBytes(imageBytes, fileName);
//                    
//                    // Cập nhật message với URL từ Cloudinary
//                    message.setImageUrl(imageUrl);
//                    message.setImageFileName(fileName);
//                    message.setImageBase64(null); // Xóa Base64 để không lưu vào DB
//                    message.setContent(""); // Hoặc có thể set "[Đã gửi ảnh]"
//                    
//                    System.out.println("[SERVER] Image uploaded successfully: " + imageUrl);
//                    
//                } catch (Exception e) {
//                    System.err.println("[SERVER] Error uploading image to Cloudinary: " + e.getMessage());
//                    e.printStackTrace();
//                    // Vẫn tiếp tục xử lý message bình thường (không có ảnh)
//                    message.setHasImage(false);
//                    message.setImageBase64(null);
//                }
//            }
//            
//            
//            // ========== XỬ LÝ MESSAGE BÌNH THƯỜNG ==========
//            // 1. Tìm hoặc tạo conversation
//            Conversation convo = findOrCreateDirectConversation(friendId);
//            
//            if (convo == null) {
//                System.err.println("[SERVER] Failed to create conversation!");
//                return;
//            }
//            
//            System.out.println("[SERVER] Using conversation ID: " + convo.getId());
//            
//            // 2. Hoàn thiện object Message
//            message.setConversationId(convo.getId());
//            message.setSenderId(currentUser.getId());
//            
//            // 3. Lưu vào CSDL
//            Message savedMessage = messageDAO.createMessage(message);
//            
//            if (savedMessage != null) {
//                System.out.println("[SERVER] Message saved: " + currentUser.getUsername() 
//                    + " -> user ID " + friendId);
//                if (savedMessage.hasImage()) {
//                    System.out.println("[SERVER] Image URL: " + savedMessage.getImageUrl());
//                }
//                
//                // 4. Gửi cho người nhận (nếu họ online)
//                Client friendClient = server.getHandlerByUserId(friendId);
//                if (friendClient != null) {
//                    friendClient.sendPacket(new Packet(MessageType.MESSAGE_RECEIVE, savedMessage));
//                    System.out.println("[SERVER] Message delivered to online user");
//                } else {
//                    System.out.println("[SERVER] Recipient is offline, message saved to DB");
//                }
//            } else {
//                System.err.println("[SERVER] Failed to save message to database");
//            }
//            
//        } catch (Exception e) {
//            System.err.println("[SERVER] Error in handleMessageSend: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    private void handleHistoryRequest(int conversationId) {
        try {
//            Conversation convo = findOrCreateDirectConversation(conversationId);
//            List<Message> history = messageDAO.findMessagesByConversationId(convo.getId());
//            
        	System.out.println("[SERVER] Fetching history for convo ID: " + conversationId);
        	
        	List<Message> history = messageDAO.findMessagesByConversationId(conversationId);
        	System.out.println("[SERVER] Sending " + history.size() 
            + " messages to " + currentUser.getUsername());
        	
            sendPacket(new Packet(MessageType.HISTORY_RESPONSE, history));
            
        } catch (Exception e) {
            e.printStackTrace();
            sendPacket(new Packet(MessageType.HISTORY_RESPONSE, Collections.emptyList()));
        }
    }
    
    private void handleGroupCreateRequest(Map<String, Object> payload) {
        try {
            String groupName = (String) payload.get("groupName");
            List<Integer> memberIds = (List<Integer>) payload.get("memberIds");

            // 1. Kiểm tra dữ liệu
            if (groupName == null || groupName.trim().isEmpty()) {
                sendPacket(new Packet(MessageType.GROUP_CREATE_FAILURE, "Tên nhóm không được để trống."));
                return;
            }
            if (memberIds == null || memberIds.isEmpty()) {
                sendPacket(new Packet(MessageType.GROUP_CREATE_FAILURE, "Phải chọn ít nhất 1 thành viên."));
                return;
            }

            // 2. Thêm người tạo nhóm vào danh sách (nếu chưa có)
            if (!memberIds.contains(currentUser.getId())) {
                memberIds.add(currentUser.getId());
            }

            System.out.println("[SERVER] User " + currentUser.getUsername() 
                + " creating group '" + groupName 
                + "' with " + memberIds.size() + " members.");

            // 3. Tạo Conversation mới trong DB
            // (Giả sử bạn có hàm createConversation(name, type) trong DAO)
            Conversation newGroup = conversationDAO.createConversation(groupName, ConversationType.GROUP);
            if (newGroup == null) {
                throw new SQLException("Không thể tạo conversation trong CSDL.");
            }

            // 4. Thêm tất cả thành viên vào bảng ConversationMember
            for (int memberId : memberIds) {
                memberDAO.addMember(newGroup.getId(), memberId);
            }
            
            System.out.println("[SERVER] Group created ID: " + newGroup.getId() + ". Notifying members...");

            // 5. Gửi thông báo thành công cho Người tạo nhóm
            sendPacket(new Packet(MessageType.GROUP_CREATE_SUCCESS, newGroup));

            // 6. Gửi thông báo "bạn vừa được thêm vào nhóm" cho các thành viên khác
            // (Họ sẽ tự động tải lại danh sách hội thoại)
            Packet notificationPacket = new Packet(MessageType.GROUP_CREATED_NOTICE, newGroup);
            
            for (int memberId : memberIds) {
                if (memberId != currentUser.getId()) { // Không gửi cho chính mình
                    Client memberClient = server.getHandlerByUserId(memberId);
                    if (memberClient != null) { // Nếu thành viên đang online
                        memberClient.sendPacket(notificationPacket);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[SERVER] Lỗi khi tạo nhóm: " + e.getMessage());
            e.printStackTrace();
            sendPacket(new Packet(MessageType.GROUP_CREATE_FAILURE, "Lỗi server khi tạo nhóm."));
        }
    }
 // ============================================================
    // ================= XỬ LÝ HỘI THOẠI (MỚI) ====================
    // ============================================================

    private void handleConversationListRequest() {
        try {	
            // Lấy tất cả hội thoại (cả 1-1 và nhóm) mà user này tham gia
            List<Conversation> conversations = conversationDAO.getConversationsByUserId(currentUser.getId());

            for (Conversation convo : conversations) {
                if (convo.getType() == ConversationType.DIRECT) {
                    // Tìm tên của người bạn chat
//                    String friendName = conversationDAO.getDirectConversationName(convo.getId(), currentUser.getId());
                	String friendName = conversationDAO.getDirectConversationName(convo.getId(), currentUser.getId());
                    convo.setName(friendName);
                    int partnerId = memberDAO.getPartnerId(convo.getId(), currentUser.getId());
                    
                    if (partnerId != -1) {
                        boolean isOnline = (server.getHandlerByUserId(partnerId) != null);
                        convo.setOnline(isOnline); // Set trạng thái vào object trước khi gửi
                        
                        // Debug log để kiểm tra
                        if (isOnline) {
                            System.out.println("[SERVER] Conversation " + convo.getId() + ": User " + partnerId + " is ONLINE. Marking as online.");
                        }
                    }
                    }
            }
            
            System.out.println("[SERVER] Sending " + conversations.size() 
                + " conversations to " + currentUser.getUsername());
            
            sendPacket(new Packet(MessageType.CONVERSATION_LIST_RESPONSE, conversations));
            
        } catch (Exception e) {
            e.printStackTrace();
            sendPacket(new Packet(MessageType.CONVERSATION_LIST_RESPONSE, Collections.emptyList()));
        }
    }
    // ============================================================
    // ====================== HÀM HỖ TRỢ ==========================
    // ============================================================

    private Conversation findOrCreateDirectConversation(int friendId) throws SQLException {
        try {
            // ✅ Kiểm tra conversationDAO và memberDAO không null
            if (conversationDAO == null) {
                System.err.println("[SERVER] conversationDAO is NULL!");
                conversationDAO = new ConversationDAO();
            }
            
            ConversationMemberDAO memberDAO = new ConversationMemberDAO();
            
            System.out.println("[SERVER] Finding conversation between " 
                + currentUser.getId() + " and " + friendId);
            
            // Tìm xem có convo 1-1 nào đã tồn tại chưa
            Conversation convo = conversationDAO.findDirectConversation(currentUser.getId(), friendId);
            
            if (convo == null) {
                System.out.println("[SERVER] No existing conversation, creating new one");
                
                // Nếu chưa có, tạo mới
                convo = conversationDAO.createConversation(null, ConversationType.DIRECT);
                
                if (convo == null) {
                    System.err.println("[SERVER] Failed to create conversation!");
                    return null;
                }
                
                System.out.println("[SERVER] Created conversation ID: " + convo.getId());
                
                // Thêm 2 thành viên vào
                boolean member1Added = memberDAO.addMember(convo.getId(), currentUser.getId());
                boolean member2Added = memberDAO.addMember(convo.getId(), friendId);
                
                System.out.println("[SERVER] Added members: user " + currentUser.getId() 
                    + " = " + member1Added + ", user " + friendId + " = " + member2Added);
            } else {
                System.out.println("[SERVER] Found existing conversation ID: " + convo.getId());
            }
            
            return convo;
            
        } catch (SQLException e) {
            System.err.println("[SERVER] SQL Error in findOrCreateDirectConversation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("[SERVER] Unexpected error in findOrCreateDirectConversation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public void sendPacket(Packet packet) {
        try {
            if (out != null) {
                // Debug logging
                System.out.println("[SERVER] sendPacket: type=" + packet.getType() 
                    + ", payload type=" + (packet.getPayload() != null ? packet.getPayload().getClass().getName() : "null")
                    + ", payload=" + packet.getPayload());
                
                synchronized (out) {
                    out.writeObject(packet);
                    out.flush();
                    out.reset();
                }
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error sending packet to " 
                + (currentUser != null ? currentUser.getUsername() : "unknown") 
                + ": " + e.getMessage());
        }
    }

    private void cleanup() {
        if (currentUser != null) {
            try {
                userDAO.setUserOnline(currentUser.getId(), false);
                System.out.println("[SERVER] User logged out: " + currentUser.getUsername());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            server.removeOnlineUser(currentUser.getId());
            currentUser = null;
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}