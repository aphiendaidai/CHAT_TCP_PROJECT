package View;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import Controll.ClientView;
import Controll.MessageType;
import Controll.Packet;
import Model.FriendSearchResult;
import Model.User;

public class MainFrame extends JFrame {
    private ClientView clientService;
    private Login loginView;
    private Register registerView;
    private JPanel mainChatPanel;

    private CardLayout mainCardLayout;
    private JPanel mainContainer;

    private SidebarPanel sidebar;
    private ChatListPanel chatList;

    private JPanel chatAreaContainer;
    private CardLayout chatCardLayout;

    private HashMap<String, ChatPanel> chatPanels;
    
    private List<User> currentFriendList;
    private List<Model.Conversation> currentConversations; // Lưu conversations để set userId sau
    
    private javax.swing.JDialog inCallDialog; // Biến quản lý cửa sổ cuộc gọi
    
    private JLabel remoteVideoScreen;
    
    private boolean sidebarVisible = true;

    public static final String LOGIN_PANEL_ID = "LOGIN";
    public static final String REGISTER_PANEL_ID = "REGISTER";
    public static final String MAIN_CHAT_ID = "MAIN_CHAT";
    public static final String WELCOME_PANEL_ID = "WELCOME_SCREEN";

    public MainFrame() {
        setTitle("Zalo Chat (Java Swing)");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainContainer = new JPanel();
        mainCardLayout = new CardLayout();
        mainContainer.setLayout(mainCardLayout);
        add(mainContainer);

        clientService = ClientView.getInstance();
        clientService.setOnPacketReceived(this::handlePacket);

        if (!clientService.connect()) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối tới Server!", 
                "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        loginView = new Login();
        registerView = new Register();

        loginView.addLoginListener(e -> handleLogin());
        loginView.addGoToRegisterListener(e -> showPanel(REGISTER_PANEL_ID));

        registerView.addRegisterListener(e -> handleRegister());
        registerView.addBackToLoginListener(e -> showPanel(LOGIN_PANEL_ID));

        mainContainer.add(loginView, LOGIN_PANEL_ID);
        mainContainer.add(registerView, REGISTER_PANEL_ID);

        mainCardLayout.show(mainContainer, LOGIN_PANEL_ID);

        setVisible(true);
    }

    // ============================================================
    // ============== GIAO DIỆN CHÍNH SAU ĐĂNG NHẬP ===============
    // ============================================================

    private JPanel createMainChatPanel(User user) {
        JPanel mainPanel = new JPanel(new BorderLayout());

        sidebar = new SidebarPanel(user);
        sidebar.setCreateGroupCallback(this::showCreateGroupDialog);
        
        chatCardLayout = new CardLayout();
        chatAreaContainer = new JPanel(chatCardLayout);
        chatPanels = new HashMap<>();

        chatList = new ChatListPanel(this::showChatPanel, this::handleSearchUserRequest, this::handleAddFriendRequest);
        
        // Thêm nút toggle sidebar vào ChatListPanel
        chatList.setToggleSidebarCallback(this::toggleSidebar);

        WelcomePanel welcomePanel = new WelcomePanel();
        chatAreaContainer.add(welcomePanel, WELCOME_PANEL_ID);
        chatCardLayout.show(chatAreaContainer, WELCOME_PANEL_ID);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(chatList, BorderLayout.WEST);
        centerPanel.add(chatAreaContainer, BorderLayout.CENTER);

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        return mainPanel;
    }
    
    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        
        if (sidebarVisible) {
            sidebar.setVisible(true);
        } else {
            sidebar.setVisible(false);
        }
        
        mainChatPanel.revalidate();
        mainChatPanel.repaint();
    }

    // ============================================================
    // ====================== HÀM XỬ LÝ LOGIN =====================
    // ============================================================

    private void handleLogin() {
        String username = loginView.getUsername();
        String password = loginView.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", 
                "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        clientService.login(username, password);
    }

    private void handleRegister() {
        String phoneNumber = registerView.getPhoneNumber();
        String username = registerView.getUsername();
        String password = registerView.getPassword();
        String confirm = registerView.getConfirmPassword();

        if (phoneNumber.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", 
                "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu nhập lại không khớp!", 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        clientService.register(username, password, phoneNumber);
    }

    // ============================================================
    // ===================== NHẬN GÓI TỪ SERVER ===================
    // ============================================================

    private void handlePacket(Packet packet) {
        SwingUtilities.invokeLater(() -> {
            MessageType type = packet.getType();
            
            System.out.println("[MainFrame] Received packet: type=" + type 
                + ", payload type=" + (packet.getPayload() != null ? packet.getPayload().getClass().getName() : "null"));
            
            switch (type) {
                case LOGIN_SUCCESS:
                    User loggedInUser = (User) packet.getPayload();
                    onLoginSuccess(loggedInUser);
                    break;

                case LOGIN_FAILURE:
                    JOptionPane.showMessageDialog(this, (String) packet.getPayload(),
                            "Lỗi Đăng nhập", JOptionPane.ERROR_MESSAGE);
                    break;

                case REGISTER_SUCCESS:
                    JOptionPane.showMessageDialog(this, (String) packet.getPayload(),
                            "Đăng ký thành công", JOptionPane.INFORMATION_MESSAGE);
                    showPanel(LOGIN_PANEL_ID);
                    break;

                case REGISTER_FAILURE:
                    JOptionPane.showMessageDialog(this, (String) packet.getPayload(),
                            "Lỗi Đăng ký", JOptionPane.ERROR_MESSAGE);
                    break;

                case FRIEND_REQUEST_RECEIVED:
                    User sender = (User) packet.getPayload();
                    JOptionPane.showMessageDialog(this, 
                        sender.getUsername() + " đã gửi lời mời kết bạn!",
                        "Lời mời kết bạn", JOptionPane.INFORMATION_MESSAGE);
                    clientService.requestPendingFriendRequests();
                    break;

                case FRIEND_REQUEST_SUCCESS:
                    JOptionPane.showMessageDialog(this, 
                        (String) packet.getPayload(),
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    break;

                case FRIEND_REQUEST_FAILURE:
                    JOptionPane.showMessageDialog(this, 
                        (String) packet.getPayload(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    break;

                case FRIEND_REQUEST_ACCEPTED:
                    User acceptedFriend = (User) packet.getPayload();
                    JOptionPane.showMessageDialog(this, 
                        acceptedFriend.getUsername() + " đã chấp nhận lời mời kết bạn!");
                    clientService.requestFriendList();
                    break;

                case FRIEND_LIST_RESPONSE:
                    @SuppressWarnings("unchecked")
                    List<User> friends = (List<User>) packet.getPayload();
                    
                    this.currentFriendList = friends != null ? friends : new ArrayList<>();
                    
                    clientService.setFriendList(friends);
                    if (chatList != null && friends != null) {
                        setDirectConversationUserIds(friends);
                    }
                    break;
                    
                case CONVERSATION_LIST_RESPONSE:
                    @SuppressWarnings("unchecked")
                    List<Model.Conversation> conversations = (List<Model.Conversation>) packet.getPayload();
                    System.out.println("[MainFrame] Received CONVERSATION_LIST_RESPONSE with " 
                        + (conversations != null ? conversations.size() : 0) + " conversations");
                    
                    this.currentConversations = conversations != null ? conversations : new ArrayList<>();
                    
                    // Request friend list để map userId cho DIRECT conversations
                    clientService.requestFriendList();
                    
                    if (chatList != null) {
                        chatList.setConversationList(conversations);
                        
                        if (currentFriendList != null && !currentFriendList.isEmpty()) {
                            setDirectConversationUserIds(currentFriendList);
                        }
                    }
                    break; // ✅ THÊM break; Ở ĐÂY!

                case PENDING_FRIEND_REQUESTS_RESPONSE:
                    Object payload = packet.getPayload();
                    System.out.println("[MainFrame] Received PENDING_FRIEND_REQUESTS_RESPONSE, payload type: " 
                        + (payload != null ? payload.getClass().getName() : "null"));
                    
                    if (payload instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<?> rawList = (List<?>) payload;
                        System.out.println("[MainFrame] List size: " + rawList.size());
                        
                        List<User> pendingRequests = new ArrayList<>();
                        for (Object item : rawList) {
                            if (item instanceof User) {
                                pendingRequests.add((User) item);
                            } else {
                                System.err.println("[MainFrame] Warning: PENDING_FRIEND_REQUESTS_RESPONSE contains non-User object: " 
                                    + (item != null ? item.getClass().getName() : "null") + " - " + item);
                            }
                        }
                        System.out.println("[MainFrame] Filtered " + pendingRequests.size() + " User objects from " + rawList.size() + " total items");
                        
                        if (sidebar != null) {
                            sidebar.updatePendingRequests(pendingRequests);
                        }
                    } else {
                        System.err.println("[MainFrame] Error: PENDING_FRIEND_REQUESTS_RESPONSE payload is not a List");
                    }
                    break;

                case USER_SEARCH_RESPONSE:
                    FriendSearchResult searchResult = (FriendSearchResult) packet.getPayload();
                    if (chatList != null) {
                        chatList.showSearchResult(searchResult);
                    }
                    break;

                case HISTORY_RESPONSE:
                    @SuppressWarnings("unchecked")
                    List<Model.Message> history = (List<Model.Message>) packet.getPayload();
                    handleHistoryResponse(history);
                    break;

                case MESSAGE_RECEIVE:
                    Model.Message newMessage = (Model.Message) packet.getPayload();
                    handleIncomingMessage(newMessage);
                    break;
                    
                case GROUP_CREATE_SUCCESS:
                    JOptionPane.showMessageDialog(this, "Tạo nhóm thành công!", 
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    // Tải lại danh sách hội thoại
                    clientService.requestConversationList();
                    break;
                
                case GROUP_CREATE_FAILURE:
                    JOptionPane.showMessageDialog(this, (String) packet.getPayload(), 
                            "Lỗi tạo nhóm", JOptionPane.ERROR_MESSAGE);
                    break;
                    
                case USER_ONLINE_NOTICE:
                    int onlineUserId = (Integer) packet.getPayload();
                    System.out.println("[INFO] User " + onlineUserId + " vừa online!");
                    if (chatList != null) {
                        chatList.updateUserStatus(onlineUserId, true);
                    }
                    break;

                case USER_OFFLINE_NOTICE:
                    int offlineUserId = (Integer) packet.getPayload();
                    System.out.println("[INFO] User " + offlineUserId + " vừa offline!");
                    if (chatList != null) {
                        chatList.updateUserStatus(offlineUserId, false);
                    }
                    break;
                    
                case CALL_REQUEST:
                    // 1. Nhận yêu cầu gọi từ người khác
                    // Payload chứa IP của người gọi (Server gửi về)
                    // Packet cần có senderId để biết ai gọi
                    String callerIP = (String) packet.getPayload();
                    int callerId = packet.getSenderId(); // Giả sử Packet có hàm này
                    
                    // Tìm tên người gọi trong danh sách bạn bè để hiển thị cho đẹp
                    String callerName = "User " + callerId;
                    for(User u : currentFriendList) {
                        if(u.getId() == callerId) {
                            callerName = u.getUsername();
                            break;
                        }
                    }

                    // Hiển thị hộp thoại hỏi
                    int choice = JOptionPane.showConfirmDialog(this,
                        "📞 " + callerName + " đang gọi cho bạn.\nBạn có muốn nghe không?",
                        "Cuộc gọi đến",
                        JOptionPane.YES_NO_OPTION);

                    if (choice == JOptionPane.YES_OPTION) {
                        // Đồng ý -> Gọi ClientView để gửi tin chấp nhận và bật UDP
                        clientService.acceptCall(callerId, callerIP);
                        
                        // Hiện cửa sổ đang gọi
                        showInCallDialog(callerName, callerId, false);
                    } else {
                        // Từ chối
                        clientService.rejectCall(callerId);
                    }
                    break;

                 // Trong handlePacket của MainFrame

                case CALL_ACCEPT:
                    String payload1 = (String) packet.getPayload(); // Ví dụ: "192.168.1.5:5000:6000"
                    
                    // Kiểm tra xem payload có chứa 2 dấu hai chấm không (Dấu hiệu của Video: IP:PortA:PortV)
                    if (payload1.split(":").length == 3) {
                        // === TRƯỜNG HỢP GỌI VIDEO ===
                        JOptionPane.showMessageDialog(this, "✅ Đối phương đã nhận cuộc gọi Video!");
                        
                        // 1. Tìm lại cái cửa sổ Video đang mở (Cái màn hình đen bạn mở lúc bấm nút gọi)
                        // (Giả sử bạn đã lưu nó vào biến toàn cục inCallDialog khi bấm gọi)
                        if (inCallDialog != null && inCallDialog.isVisible()) {
                            // Không cần tạo mới, dùng luôn cái đang mở
                        } else {
                            // Phòng hờ: Nếu lỡ tắt rồi thì mở lại
                            showInCallDialog("Người bạn", 0, true);
                        }

                        // 2. QUAN TRỌNG NHẤT: Bắt đầu luồng Video và truyền cái màn hình đen (remoteVideoScreen) vào
                        ClientView.getInstance().onVideoCallAccepted(payload1, remoteVideoScreen);
                        
                    } else {
                        // === TRƯỜNG HỢP GỌI THOẠI (AUDIO) ===
                        JOptionPane.showMessageDialog(this, "✅ Kết nối thoại thành công!");
                        ClientView.getInstance().onCallAccepted(payload1);
                        
                        // Nếu chưa mở dialog thì mở (Audio)
                        if (inCallDialog == null || !inCallDialog.isVisible()) {
                             showInCallDialog("Người bạn", 0, false);
                        }
                    }
                    break;

                case CALL_DENY:
                    // 3. Bị từ chối
                    JOptionPane.showMessageDialog(this, 
                        "❌ Cuộc gọi bị từ chối hoặc người dùng đang bận.", 
                        "Thông báo", 
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                    
                    // Nếu đang hiện dialog "Đang gọi..." thì tắt đi (bạn cần xử lý thêm logic này nếu muốn)
                    if (inCallDialog != null) inCallDialog.dispose();
                    break;

                case CALL_END:
                    // 4. Kết thúc cuộc gọi
                    clientService.endCall(0); // ID không quan trọng lúc nhận tin END
                    JOptionPane.showMessageDialog(this, "Cuộc gọi đã kết thúc.");
                    
                    // Đóng cửa sổ gọi
                    if (inCallDialog != null) {
                        inCallDialog.dispose();
                    }
                    break;
                    
//                case VIDEO_CALL_REQUEST: // (Nhớ thêm vào Enum MessageType)
//                    String callerIP1 = (String) packet.getPayload(); // Payload chỉ chứa IP (Server đã tách)
//                    // Nhưng đợi đã! Payload của Video Call phức tạp hơn: "IP:AudioPort:VideoPort"
//                    // Server cần chuyển nguyên xi payload "IP:PortA:PortV" xuống
//                    
//                    String payloadStr = (String) packet.getPayload(); // "192.168.1.5:5000:6000"
//                    String[] parts = payloadStr.split(":");
//                    String targetIP = parts[0];
//                    int audioPort = Integer.parseInt(parts[1]);
//                    int videoPort = Integer.parseInt(parts[2]);
//                    System.out.println("[ClientView] Chấp nhận Video Call từ: " + targetIP);
//                    int callerId1 = packet.getSenderId();
//
//                    int choice1 = JOptionPane.showConfirmDialog(this, 
//                        "🎥 " + packet.getSenderName() + " muốn gọi Video.\nNghe không?", 
//                        "Video Call", JOptionPane.YES_NO_OPTION);
//
//                    if (choice1 == JOptionPane.YES_OPTION) {
//                        // 1. Hiện Dialog Video lên trước
//                        showInCallDialog(packet.getSenderName(), callerId1, true);
//                        
//                        // 2. Gửi chấp nhận (ClientView sẽ tự tạo 2 cổng của mình để gửi lại)
//                        // (Bạn cần viết thêm hàm acceptVideoCall trong ClientView tương tự acceptCall)
//                        // clientService.acceptVideoCall(callerId, targetIP, audioPort, videoPort); 
//                        
//                        // 3. Bắt đầu hiển thị Video lên cái Label vừa tạo trong Dialog
//                        clientService.startVideoChat(targetIP, audioPort, videoPort, remoteVideoScreen);
//                    }
//                    break;
                case VIDEO_CALL_REQUEST:
                    String payloadStr = (String) packet.getPayload(); // "IP:5000:6000"
                    int callerId1 = packet.getSenderId();
                    String callerName1 = packet.getSenderName();

                    int choice1 = JOptionPane.showConfirmDialog(this, 
                        "🎥 " + callerName1 + " muốn gọi Video.\nNghe không?", 
                        "Video Call", JOptionPane.YES_NO_OPTION);

                    if (choice1 == JOptionPane.YES_OPTION) {
                        // 1. Hiện cửa sổ Video lên trước (để khởi tạo JLabel)
                        showInCallDialog(callerName1, callerId1, true);
                        
                        // 2. Gọi hàm ACCEPT dành riêng cho Video (Hàm vừa viết ở Bước 1)
                        ClientView.getInstance().acceptVideoCall(callerId1, payloadStr);
                        
                    } else {
                        clientService.rejectCall(callerId1);
                    }
                    break;
                    
                default:
                    System.err.println("[CLIENT] Unhandled packet type: " + type);
                    break;
            }
        });
    }
    // ============================================================
    // =================== SAU KHI LOGIN THÀNH CÔNG ===============
    // ============================================================

    private void onLoginSuccess(User user) {
        setTitle("Zalo Chat - Xin chào, " + user.getUsername());
        clientService.setCurrentUser(user);

        mainChatPanel = createMainChatPanel(user);
        mainContainer.add(mainChatPanel, MAIN_CHAT_ID);
        mainCardLayout.show(mainContainer, MAIN_CHAT_ID);

        // ✅ Gửi cùng lúc: danh sách HỘI THOẠI + lời mời đang chờ
//        clientService.requestConversationList();
//        clientService.requestPendingFriendRequests();
        clientService.requestConversationList();
        
        // Đợi 100ms rồi gửi request 2
        new Thread(() -> {
            try {
                Thread.sleep(100);
                clientService.requestPendingFriendRequests();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ============================================================
    // ====================== TIỆN ÍCH KHÁC =======================
    // ============================================================

    private void showPanel(String panelId) {
        mainCardLayout.show(mainContainer, panelId);
    }

    private void showChatPanel(String conversationIdStr, String conversationName) {
        // ✅ Nếu chưa có ChatPanel cho conversation này, tạo mới
        ChatPanel panelToSwitch = chatPanels.get(conversationIdStr);
        
        if (panelToSwitch == null) {
            // Tạo ChatPanel mới với conversationId (KHÔNG phải friendId nữa)
            int conversationId = Integer.parseInt(conversationIdStr);
            panelToSwitch = new ChatPanel(conversationId, conversationName);
            
            // Lưu vào HashMap
            chatPanels.put(conversationIdStr, panelToSwitch);
            
            // Thêm vào CardLayout
            chatAreaContainer.add(panelToSwitch, conversationIdStr);
            
            System.out.println("[MainFrame] Created new ChatPanel for: " + conversationName + " (Conversation ID: " + conversationIdStr + ")");
            
            if (chatList != null) {
                int friendId = chatList.getUserIdForConversation(conversationId);
                if (friendId != -1) {
                    panelToSwitch.setFriendId(friendId);
                    System.out.println("[MainFrame] Set friendId " + friendId + " for ChatPanel " + conversationName);
                } else {
                    System.err.println("[MainFrame] Warning: Could not find friendId for conversation " + conversationId);
                }
            }
            
        }
        
 
        
        // Hiển thị ChatPanel
        panelToSwitch.setHeaderTitle(conversationName);	
        chatCardLayout.show(chatAreaContainer, conversationIdStr);
        
        System.out.println("[MainFrame] Switched to chat with: " + conversationName);
    }

    private void handleAddFriendRequest(String phoneNumber) {
        User currentUser = clientService.getCurrentUser();
        String currentPhone = currentUser != null ? currentUser.getPhoneNumber() : null;
        if (currentPhone != null && currentPhone.equals(phoneNumber)) {
            JOptionPane.showMessageDialog(this, "Bạn không thể tự kết bạn với chính mình!",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        clientService.sendFriendRequest(phoneNumber);
    }
    
    private void handleSearchUserRequest(String phoneNumber) {
        clientService.searchUserByPhoneNumber(phoneNumber);
    }
    
    /**
     * Set userId cho DIRECT conversations bằng cách so sánh tên conversation với tên friend
     * (Vì server đã set tên conversation = username của friend trong DIRECT conversation)
     */
    private void setDirectConversationUserIds(List<User> friends) {
        if (chatList == null || currentConversations == null || friends == null) {
            return;
        }
        
        // Tạo map username -> userId để tìm nhanh
        Map<String, Integer> usernameToUserIdMap = new HashMap<>();
        for (User friend : friends) {
            if (friend.getUsername() != null) {
                usernameToUserIdMap.put(friend.getUsername(), friend.getId());
            }
        }
        
        // Duyệt qua các conversations và set userId cho DIRECT conversations
        for (Model.Conversation convo : currentConversations) {
            if (convo.getType() == Model.ConversationType.DIRECT) {
                String conversationName = convo.getName();
                Integer userId = usernameToUserIdMap.get(conversationName);
                
                if (userId != null) {
                    chatList.setDirectConversationUserId(convo.getId(), userId);
                    System.out.println("[MainFrame] Set userId " + userId + " for DIRECT conversation " 
                        + convo.getId() + " (name: " + conversationName + ")");
                } else {
                    System.out.println("[MainFrame] Warning: Could not find userId for DIRECT conversation " 
                        + convo.getId() + " (name: " + conversationName + ")");
                }
            }
        }
    }
    
    
 // ✅ Hàm xử lý lịch sử tin nhắn
    private void handleHistoryResponse(List<Model.Message> history) {
        if (history == null || history.isEmpty()) {
            System.out.println("[MainFrame] No message history received");
            return;
        }

        // Lấy tin nhắn đầu tiên để xác định friendId
        Model.Message firstMsg = history.get(0);
        int conversationId = firstMsg.getConversationId();
        
        // Xác định ID của bạn bè (người còn lại trong cuộc hội thoại)
//        int friendId = (firstMsg.getSenderId() == currentUser.getId()) 
//                       ? firstMsg.getReceiverId() 
//                       : firstMsg.getSenderId();
        
        String conversationIdStr = String.valueOf(conversationId);
        ChatPanel chatPanel = chatPanels.get(conversationIdStr);
        
        if (chatPanel != null) {
            // Xóa tin nhắn cũ trước khi load lịch sử
            chatPanel.clearMessages();
            User currentUser = clientService.getCurrentUser(); // Lấy user hiện tại
            // Thêm từng tin nhắn vào UI
            for (Model.Message msg : history) {
                boolean isSelf = (msg.getSenderId() == currentUser.getId());
                chatPanel.addMessageToUI(msg, isSelf);
            }
            
            System.out.println("[MainFrame] Loaded " + history.size() + " messages for friend ID: " + conversationId);
        } else {
            System.err.println("[MainFrame] ChatPanel not found for friend ID: " + conversationId);
        }
    }

    // ✅ Hàm xử lý tin nhắn mới đến
//    private void handleIncomingMessage(Model.Message message) {
//        // Xác định ID người gửi (không phải mình)
//        int conversationId = message.getSenderId();
//        String conversationIdStr = String.valueOf(conversationId);
//        
//        ChatPanel chatPanel = chatPanels.get(conversationIdStr);
//        
//        if (chatPanel != null) {
//            // Thêm tin nhắn vào ChatPanel đang mở
//            chatPanel.addMessageToUI(message, false);
//            System.out.println("[MainFrame] Message displayed from user ID: " + conversationId);
//        } else {
//            // Nếu chưa mở ChatPanel, lưu vào queue hoặc hiển thị notification
//            System.out.println("[MainFrame] Message received but ChatPanel not open for user ID: " + conversationId);
//            
//            // TODO: Có thể hiển thị badge hoặc notification ở đây
//            // Ví dụ: chatList.showNewMessageBadge(senderId);
//        }
//    }
    private void handleIncomingMessage(Model.Message message) {
        // ✅ ĐÚNG: Dùng conversationId từ message
        int conversationId = message.getConversationId();
        String conversationIdStr = String.valueOf(conversationId);

        ChatPanel chatPanel = chatPanels.get(conversationIdStr);

        if (chatPanel != null) {
            // Thêm tin nhắn vào ChatPanel đang mở
            chatPanel.addMessageToUI(message, false);
            System.out.println("[MainFrame] Message displayed in conversation ID: " + conversationId);
        } else {
            // Nếu chưa mở ChatPanel, hiển thị notification
            System.out.println("[MainFrame] Message received but ChatPanel not open for conversation ID: " + conversationId);
            
            // TODO: Hiển thị badge hoặc notification
            // chatList.showNewMessageBadge(conversationId);
        }
    }
    
    private void showCreateGroupDialog() {
        if (this.currentFriendList == null || this.currentFriendList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn cần có bạn bè để tạo nhóm!", 
                    "Không thể tạo nhóm", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Tạo panel tùy chỉnh cho hộp thoại ---
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Panel nhập Tên nhóm
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel("Tên nhóm:"), BorderLayout.WEST);
        JTextField groupNameField = new JTextField();
        groupNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        namePanel.add(groupNameField, BorderLayout.CENTER);
        dialogPanel.add(namePanel, BorderLayout.NORTH);

        // 2. Panel danh sách bạn bè (dạng checkbox)
        JPanel friendListPanel = new JPanel();
        friendListPanel.setLayout(new BoxLayout(friendListPanel, BoxLayout.Y_AXIS));
        
        // Dùng HashMap để lưu trữ JCheckBox tương ứng với mỗi User
        HashMap<User, JCheckBox> friendCheckboxes = new HashMap<>();

        for (User friend : this.currentFriendList) {
            JCheckBox checkBox = new JCheckBox(friend.getUsername());
            checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            friendListPanel.add(checkBox);
            friendListPanel.add(Box.createVerticalStrut(5));
            friendCheckboxes.put(friend, checkBox); // Lưu lại
        }

        // Thêm thanh cuộn
        JScrollPane scrollPane = new JScrollPane(friendListPanel);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chọn thành viên"));
        dialogPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Hiển thị hộp thoại ---
        int result = JOptionPane.showConfirmDialog(this, dialogPanel, "Tạo nhóm mới",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            // Người dùng nhấn "OK", xử lý dữ liệu
            String groupName = groupNameField.getText().trim();
            if (groupName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên nhóm không được để trống!", 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Lấy danh sách ID của bạn bè được chọn
            List<Integer> selectedMemberIds = new ArrayList<>();
            for (Map.Entry<User, JCheckBox> entry : friendCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    selectedMemberIds.add(entry.getKey().getId());
                }
            }

            if (selectedMemberIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Bạn phải chọn ít nhất một người bạn!", 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // --- Gửi yêu cầu tạo nhóm lên server ---
            System.out.println("[MainFrame] Yêu cầu tạo nhóm '" + groupName 
                    + "' với thành viên: " + selectedMemberIds);
            
            // (Bạn cần thêm hàm này trong ClientView)
            clientService.createGroup(groupName, selectedMemberIds); 
        }
    }
    
    /**
     * Hiển thị cửa sổ trạng thái cuộc gọi
     */
    public void showInCallDialog(String partnerName, int partnerId, boolean isVideoCall) {
        if (inCallDialog != null && inCallDialog.isVisible()) return;

        inCallDialog = new javax.swing.JDialog(this, isVideoCall ? "Video Call" : "Voice Call", false);
        
        // Nếu là Video Call thì cửa sổ to hơn
        inCallDialog.setSize(isVideoCall ? 660 : 300, isVideoCall ? 500 : 150);
        inCallDialog.setLayout(new BorderLayout());
        inCallDialog.setLocationRelativeTo(this);
        inCallDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        // --- PHẦN HIỂN THỊ VIDEO (Nếu có) ---
        if (isVideoCall) {
            // Màn hình hiển thị video đối phương
            remoteVideoScreen = new JLabel();
            remoteVideoScreen.setHorizontalAlignment(JLabel.CENTER);
            remoteVideoScreen.setBackground(java.awt.Color.BLACK);
            remoteVideoScreen.setOpaque(true);
            remoteVideoScreen.setPreferredSize(new Dimension(640, 480));
            
            // 
            inCallDialog.add(remoteVideoScreen, BorderLayout.CENTER);
        } else {
            // Chỉ hiện tên nếu là Audio
            JLabel lblStatus = new JLabel("<html><center>Đang nói chuyện với<br><b>" + partnerName + "</b></center></html>", JLabel.CENTER);
            lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            inCallDialog.add(lblStatus, BorderLayout.CENTER);
        }

        // --- NÚT KẾT THÚC ---
        JButton btnEndCall = new JButton("Kết thúc");
        btnEndCall.setBackground(java.awt.Color.RED);
        btnEndCall.setForeground(java.awt.Color.WHITE);
        btnEndCall.addActionListener(e -> {
            clientService.endCall(partnerId); // Gửi tín hiệu kết thúc
            if (remoteVideoScreen != null) remoteVideoScreen.setIcon(null); // Xóa hình cũ
            inCallDialog.dispose();
        });
        
        inCallDialog.add(btnEndCall, BorderLayout.SOUTH);
        inCallDialog.setVisible(true);
        
        if (isVideoCall) {
            ClientView.getInstance().setRemoteVideoLabel(remoteVideoScreen);
        }
    }
    
//    private void showInCallDialog(String partnerName, int partnerId) {
//        if (inCallDialog != null && inCallDialog.isVisible()) return;
//
//        inCallDialog = new javax.swing.JDialog(this, "Đang trong cuộc gọi", false);
//        inCallDialog.setSize(300, 150);
//        inCallDialog.setLayout(new java.awt.BorderLayout());
//        inCallDialog.setLocationRelativeTo(this);
//        inCallDialog.setAlwaysOnTop(true); // Luôn nổi lên trên
//        inCallDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE); // Chặn tắt bằng dấu X
//
//        // Label hiển thị tên
//        JLabel lblStatus = new JLabel("<html><center>đang nói chuyện với<br><b>" + partnerName + "</b></center></html>", JLabel.CENTER);
//        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 16));
//        lblStatus.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
//        
//     // Thay vì label chữ, ta thêm 1 cái Label chứa ảnh
//        JLabel videoScreen = new JLabel();
//        videoScreen.setPreferredSize(new Dimension(320, 240));
//        videoScreen.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        
//        
//        // Nút kết thúc cuộc gọi
//        javax.swing.JButton btnEndCall = new javax.swing.JButton("Kết thúc cuộc gọi");
//        btnEndCall.setBackground(new java.awt.Color(220, 53, 69)); // Màu đỏ
//        btnEndCall.setForeground(java.awt.Color.WHITE);
//        btnEndCall.setFocusPainted(false);
//        btnEndCall.setFont(new Font("Segoe UI", Font.BOLD, 14));
//        
//        btnEndCall.addActionListener(e -> {
//            // Gửi tín hiệu kết thúc
//            clientService.endCall(partnerId);
//            inCallDialog.dispose();
//        });
//        inCallDialog.add(videoScreen, BorderLayout.CENTER); // Để ở giữa
//        inCallDialog.add(lblStatus, java.awt.BorderLayout.CENTER);
//        inCallDialog.add(btnEndCall, java.awt.BorderLayout.SOUTH);
//        
//        inCallDialog.setVisible(true);
//    }

    // ============================================================
    // =========================== MAIN ===========================
    // ============================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}