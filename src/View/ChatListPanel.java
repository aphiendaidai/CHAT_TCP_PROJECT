package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import Model.Conversation;
import Model.ConversationType;
import Model.FriendSearchResult;
import Model.User;

public class ChatListPanel extends JPanel {
    
    private JPanel chatListContainer;
    private BiConsumer<String, String> onChatSelected;
    private Consumer<String> onSearchUser;
    private Consumer<String> onSendFriendRequest;
    private Runnable onToggleSidebar;
    
    // Lưu trữ các item để highlight selected
    private Map<String, JPanel> chatItemPanels = new HashMap<>();
    private JPanel currentSelectedItem = null;
    
    // Lưu trữ statusLabel để cập nhật real-time
    private Map<String, JLabel> statusLabels = new HashMap<>();
    
    // Lưu danh sách bạn bè gốc để tìm kiếm
    private List<User> allFriends;
    private List<Conversation> allConversations;
    // Lưu userMap
    private Map<Integer, User> userMap = new HashMap<>();
    
    // Lưu mapping conversationId -> userId cho DIRECT conversations
    // (Để có thể update status khi user online/offline)
    private Map<Integer, Integer> directConversationUserIdMap = new HashMap<>();
    
    private JTextField searchField;
    private JPanel searchResultContainer;
    private String pendingSearchQuery = "";
    
    // Màu sắc - Modern Dark Theme
    private final Color COLOR_BACKGROUND = new Color(42, 44, 50);
    private final Color COLOR_ITEM_HOVER = new Color(55, 58, 64);
    private final Color COLOR_ITEM_SELECTED = new Color(68, 72, 80);
    private final Color COLOR_BACKGROUND_SEARCH = new Color(35, 37, 42);
    private final Color COLOR_TEXT_PRIMARY = new Color(255, 255, 255);
    private final Color COLOR_TEXT_SECONDARY = new Color(170, 173, 178);
    private final Color COLOR_ACCENT = new Color(88, 101, 242);
    private final Color COLOR_ONLINE = new Color(67, 181, 129);
    private final Color COLOR_OFFLINE = new Color(142, 146, 151);
    
    public ChatListPanel(BiConsumer<String, String> onChatSelected, 
                         Consumer<String> onSearchUser, 
                         Consumer<String> onSendFriendRequest) {
        this.onChatSelected = onChatSelected;
        this.onSearchUser = onSearchUser;
        this.onSendFriendRequest = onSendFriendRequest;
        
        setBackground(COLOR_BACKGROUND);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 700));
        
        // ============== SEARCH BAR + TOGGLE BUTTON ==============
        JPanel searchBarPanel = new JPanel(new BorderLayout(8, 0));
        searchBarPanel.setBackground(COLOR_BACKGROUND);
        searchBarPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Nút toggle sidebar
        JButton toggleSidebarBtn = new JButton("☰");
        toggleSidebarBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        toggleSidebarBtn.setBackground(COLOR_BACKGROUND_SEARCH);
        toggleSidebarBtn.setForeground(COLOR_TEXT_PRIMARY);
        toggleSidebarBtn.setFocusPainted(false);
        toggleSidebarBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BACKGROUND_SEARCH, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        toggleSidebarBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleSidebarBtn.setPreferredSize(new Dimension(45, 45));
        toggleSidebarBtn.addActionListener(e -> {
            if (onToggleSidebar != null) {
                onToggleSidebar.run();
            }
        });
        
        // Hover effect cho nút toggle
        toggleSidebarBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                toggleSidebarBtn.setBackground(COLOR_ITEM_HOVER);
                toggleSidebarBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_ITEM_HOVER, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                toggleSidebarBtn.setBackground(COLOR_BACKGROUND_SEARCH);
                toggleSidebarBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BACKGROUND_SEARCH, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        searchBarPanel.add(toggleSidebarBtn, BorderLayout.WEST);
        
        searchField = new JTextField("🔍 Tìm kiếm");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBackground(COLOR_BACKGROUND_SEARCH);
        searchField.setForeground(COLOR_TEXT_SECONDARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(COLOR_BACKGROUND_SEARCH.getRGB()), 1, true),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        searchField.setCaretColor(COLOR_ACCENT);
        
        // Focus listener để xóa placeholder
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (searchField.getText().equals("🔍 Tìm kiếm")) {
                    searchField.setText("");
                    searchField.setForeground(COLOR_TEXT_PRIMARY);
                }
            }
            
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("🔍 Tìm kiếm");
                    searchField.setForeground(COLOR_TEXT_SECONDARY);
                }
            }
        });
        
        // DocumentListener để tìm kiếm real-time
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
            
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
            
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
            
            private void performSearch() {
                String searchText = searchField.getText().trim();
                
                // Bỏ qua nếu đang hiển thị placeholder
                if (searchText.equals("🔍 Tìm kiếm") || searchText.isEmpty()) {
                    pendingSearchQuery = "";
                    clearSearchResult();
                    // Hiển thị lại danh sách conversations gốc
                    setConversationList(allConversations);
                    return;
                }
                
                // ✅ Tìm kiếm trong CONVERSATIONS (theo tên conversation)
                if (allConversations != null && !allConversations.isEmpty()) {
                    List<Conversation> filteredConversations = allConversations.stream()
                        .filter(convo -> {
                            String name = convo.getName() != null ? convo.getName().toLowerCase() : "";
                            return name.contains(searchText.toLowerCase());
                        })
                        .collect(Collectors.toList());
                    
                    // Hiển thị kết quả tìm kiếm conversations
                    if (!filteredConversations.isEmpty()) {
                        pendingSearchQuery = "";
                        clearSearchResult();
                        // Hiển thị filtered conversations (KHÔNG ghi đè allConversations)
                        displayFilteredConversations(filteredConversations);
                    } else {
                        // Không tìm thấy trong conversations, thử tìm theo số điện thoại
                        if (isPhoneNumberCandidate(searchText)) {
                            if (!searchText.equals(pendingSearchQuery) && ChatListPanel.this.onSearchUser != null) {
                                pendingSearchQuery = searchText;
                                showSearchResultLoading(searchText);
                                ChatListPanel.this.onSearchUser.accept(searchText);
                            }
                        } else {
                            showSearchResultMessage("Không tìm thấy cuộc hội thoại phù hợp.", false);
                        }
                    }
                } else if (allFriends != null && !allFriends.isEmpty()) {
                    // Fallback: Nếu không có conversations, tìm trong friends (logic cũ)
                    List<User> filteredFriends = allFriends.stream()
                        .filter(friend -> {
                            String username = friend.getUsername().toLowerCase();
                            String phone = friend.getPhoneNumber() != null ? friend.getPhoneNumber() : "";
                            return username.contains(searchText.toLowerCase()) || phone.contains(searchText);
                        })
                        .collect(Collectors.toList());
                    
                    displayFriends(filteredFriends);
                    
                    if (!filteredFriends.isEmpty()) {
                        pendingSearchQuery = "";
                        clearSearchResult();
                    } else if (isPhoneNumberCandidate(searchText)) {
                        if (!searchText.equals(pendingSearchQuery) && ChatListPanel.this.onSearchUser != null) {
                            pendingSearchQuery = searchText;
                            showSearchResultLoading(searchText);
                            ChatListPanel.this.onSearchUser.accept(searchText);
                        }
                    } else {
                        showSearchResultMessage("Không tìm thấy người dùng phù hợp.", false);
                    }
                } else {
                    // Không có conversations và friends, chỉ có thể tìm theo số điện thoại
                    if (isPhoneNumberCandidate(searchText)) {
                        if (!searchText.equals(pendingSearchQuery) && ChatListPanel.this.onSearchUser != null) {
                            pendingSearchQuery = searchText;
                            showSearchResultLoading(searchText);
                            ChatListPanel.this.onSearchUser.accept(searchText);
                        }
                    } else {
                        showSearchResultMessage("Không tìm thấy kết quả phù hợp.", false);
                    }
                }
            }
        });
        
        searchBarPanel.add(searchField, BorderLayout.CENTER);
        add(searchBarPanel, BorderLayout.NORTH);
        
        // ============== SEARCH RESULT + CHAT LIST CONTAINER ==============
        searchResultContainer = new JPanel();
        searchResultContainer.setLayout(new BoxLayout(searchResultContainer, BoxLayout.Y_AXIS));
        searchResultContainer.setBackground(COLOR_BACKGROUND);
        searchResultContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        chatListContainer = new JPanel();
        chatListContainer.setLayout(new BoxLayout(chatListContainer, BoxLayout.Y_AXIS));
        chatListContainer.setBackground(COLOR_BACKGROUND);
        chatListContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(COLOR_BACKGROUND);
        scrollContent.add(searchResultContainer);
        scrollContent.add(chatListContainer);
        
        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Set callback để toggle sidebar
     */
    public void setToggleSidebarCallback(Runnable callback) {
        this.onToggleSidebar = callback;
    }
    
    /**
     * Set userId cho DIRECT conversation
     * (Được gọi từ MainFrame sau khi nhận conversation list từ server)
     * Giúp lưu mapping conversationId -> userId để update status sau này
     */
    public void setDirectConversationUserId(int conversationId, int userId) {
        directConversationUserIdMap.put(conversationId, userId);
    }
    
    /**
     * Cập nhật danh sách bạn bè
     */
    public void setFriendList(List<User> friends) {
        this.allFriends = friends;
    }
//    
 // MainFrame sẽ gọi hàm này thay vì setFriendList
    public void setConversationList(List<Conversation> conversations) {
        // ✅ QUAN TRỌNG: Lưu conversations vào allConversations để dùng cho tìm kiếm
        this.allConversations = conversations;
        
        chatListContainer.removeAll(); // Xóa cũ
        chatItemPanels.clear();
        statusLabels.clear();
        // KHÔNG clear userMap và directConversationUserIdMap vì có thể cần cho update status
        currentSelectedItem = null;

        // Bạn cần một List<Conversation> từ server
        // (Server phải tự điền tên cho chat 1-1)

        if (conversations != null && !conversations.isEmpty()) {
            for (Conversation convo : conversations) {
                JPanel item = createConversationItem(convo); // Dùng hàm mới
                chatListContainer.add(item);

                // Key bây giờ là "convo_{id}"
                chatItemPanels.put("convo_" + convo.getId(), item);
            }
        } else {
            // Hiển thị "Chưa có cuộc hội thoại nào"
            JLabel emptyLabel = new JLabel("Chưa có cuộc hội thoại nào");
            emptyLabel.setForeground(COLOR_TEXT_SECONDARY);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 15));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));
            chatListContainer.add(emptyLabel);
        }

        chatListContainer.revalidate();
        chatListContainer.repaint();
    }
    
    /**
     * Hiển thị danh sách conversations đã filter (dùng cho tìm kiếm)
     * KHÔNG ghi đè allConversations để có thể khôi phục danh sách gốc
     */
    private void displayFilteredConversations(List<Conversation> filteredConversations) {
        chatListContainer.removeAll();
        chatItemPanels.clear();
        statusLabels.clear();
        currentSelectedItem = null;

        if (filteredConversations != null && !filteredConversations.isEmpty()) {
            for (Conversation convo : filteredConversations) {
                JPanel item = createConversationItem(convo);
                chatListContainer.add(item);
                chatItemPanels.put("convo_" + convo.getId(), item);
            }
        } else {
            JLabel emptyLabel = new JLabel("Không tìm thấy kết quả");
            emptyLabel.setForeground(COLOR_TEXT_SECONDARY);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 15));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));
            chatListContainer.add(emptyLabel);
        }

        chatListContainer.revalidate();
        chatListContainer.repaint();
    }
    
    private JPanel createConversationItem(Conversation convo) {
        JPanel item = new JPanel(new BorderLayout(15, 0));
        item.setBackground(COLOR_BACKGROUND);
        item.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        String conversationName = convo.getName(); // Server phải điền tên này
        String conversationID = String.valueOf(convo.getId());

        // Avatar (có thể tùy chỉnh)
        JPanel avatarPanel;
        if (convo.getType() == ConversationType.GROUP) {
            avatarPanel = createGroupAvatar(conversationName); // Vẽ avatar nhóm (chữ cái đầu)
        } else {
            // Lấy User từ đâu đó (nếu server không gửi kèm)
            // Tạm thời vẫn dùng chữ cái đầu của tên hội thoại (tên bạn)
            avatarPanel = createUserAvatar(conversationName); // Dùng lại hàm vẽ avatar cũ
        }

        // Text panel
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(conversationName);
        nameLabel.setForeground(COLOR_TEXT_PRIMARY);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        textPanel.add(nameLabel, BorderLayout.NORTH);

        // HIỂN THỊ STATUS CHO NHÓM / BẠN BÈ
        if (convo.getType() == ConversationType.GROUP) {
            JLabel statusLabel = new JLabel("🧑‍🤝‍🧑 Chat nhóm");
            statusLabel.setForeground(COLOR_TEXT_SECONDARY);
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            textPanel.add(statusLabel, BorderLayout.CENTER);

        } else { // Là chat DIRECT
        	JLabel statusLabel = new JLabel();
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            if (convo.isOnline()) {
                statusLabel.setText("● Online");
                statusLabel.setForeground(COLOR_ONLINE); // Màu xanh
            } else {
                statusLabel.setText("○ Offline");
                statusLabel.setForeground(COLOR_OFFLINE); // Màu xám
            }

            // Lưu statusLabel với key là "convo_{id}" để update sau này
            String statusKey = "convo_" + convo.getId();
            statusLabels.put(statusKey, statusLabel);
            
            textPanel.add(statusLabel, BorderLayout.CENTER);
        }

        item.add(avatarPanel, BorderLayout.WEST);
        item.add(textPanel, BorderLayout.CENTER);

        // Mouse listeners với hover effect và selection
        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                // Chỉ đổi màu nếu item chưa được selected
                if (item != currentSelectedItem) {
                    item.setBackground(COLOR_ITEM_HOVER);
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                // Chỉ đổi màu nếu item chưa được selected
                if (item != currentSelectedItem) {
                    item.setBackground(COLOR_BACKGROUND);
                }
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                // Bỏ highlight item cũ
                if (currentSelectedItem != null) {
                    currentSelectedItem.setBackground(COLOR_BACKGROUND);
                }
                
                // Highlight item mới
                currentSelectedItem = item;
                item.setBackground(COLOR_ITEM_SELECTED);

                // Gọi callback để mở chat (Gửi CONVERSATION_ID)
                if (onChatSelected != null) {
                    // Đây là thay đổi quan trọng nhất
                    onChatSelected.accept(conversationID, conversationName);
                }
            }
        });

        return item;
    }

    /**
     * Tạo avatar cho user (chat 1-1)
     * Vẽ hình tròn với chữ cái đầu của tên
     */
    private JPanel createUserAvatar(String name) {
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                   java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                   java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                
                // Vẽ hình tròn với shadow effect
                g2.setColor(new Color(COLOR_ACCENT.getRed(), COLOR_ACCENT.getGreen(), COLOR_ACCENT.getBlue(), 200));
                g2.fillOval(1, 1, 48, 48);
                g2.setColor(COLOR_ACCENT);
                g2.fillOval(0, 0, 48, 48);
                
                // Vẽ chữ cái đầu
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                String initial = name != null && !name.isEmpty() 
                    ? name.substring(0, 1).toUpperCase() 
                    : "?";
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = (48 - fm.stringWidth(initial)) / 2;
                int y = ((48 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
            }
        };
        avatarPanel.setPreferredSize(new Dimension(48, 48));
        avatarPanel.setOpaque(false);
        return avatarPanel;
    }

    /**
     * Tạo avatar cho group chat
     * Vẽ hình tròn với màu khác (ví dụ: màu xanh lá) và icon nhóm
     */
    private JPanel createGroupAvatar(String name) {
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                   java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                   java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                
                // Màu xanh lá cho group (khác với màu accent của user)
                Color groupColor = new Color(67, 181, 129); // Màu xanh lá
                
                // Vẽ hình tròn với shadow effect
                g2.setColor(new Color(groupColor.getRed(), groupColor.getGreen(), groupColor.getBlue(), 200));
                g2.fillOval(1, 1, 48, 48);
                g2.setColor(groupColor);
                g2.fillOval(0, 0, 48, 48);
                
                // Vẽ icon nhóm hoặc chữ cái đầu
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                String initial = name != null && !name.isEmpty() 
                    ? name.substring(0, 1).toUpperCase() 
                    : "G";
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = (48 - fm.stringWidth(initial)) / 2;
                int y = ((48 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
            }
        };
        avatarPanel.setPreferredSize(new Dimension(48, 48));
        avatarPanel.setOpaque(false);
        return avatarPanel;
    }
    
    
    /**
     * Hiển thị danh sách bạn bè (dùng chung cho cả load và search)
     */
    private void displayFriends(List<User> friends) {
        chatListContainer.removeAll();
        chatItemPanels.clear();
        statusLabels.clear();
        currentSelectedItem = null;
        
        // KHÔNG clear userMap để giữ mapping khi search
        // userMap chỉ được clear khi setFriendList được gọi với danh sách mới
        if (friends == allFriends) {
            userMap.clear();
        }
        
        if (friends != null && !friends.isEmpty()) {
            for (User friend : friends) {
                JPanel item = createChatItem(friend);
                chatListContainer.add(item);
                chatItemPanels.put(String.valueOf(friend.getId()), item);
                
                // Chỉ add vào userMap khi load danh sách gốc
                if (friends == allFriends) {
                    userMap.put(friend.getId(), friend);
                }
            }
        } else {
            // Hiển thị thông báo
            JLabel emptyLabel = new JLabel(
                (allFriends == null || allFriends.isEmpty())
                    ? "Chưa có bạn bè"
                    : "Không tìm thấy kết quả"
            );
            emptyLabel.setForeground(COLOR_TEXT_SECONDARY);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 15));
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));
            chatListContainer.add(emptyLabel);
        }
        
        chatListContainer.revalidate();
        chatListContainer.repaint();
    }
    
    private void clearSearchResult() {
        if (searchResultContainer == null) {
            return;
        }
        searchResultContainer.removeAll();
        searchResultContainer.revalidate();
        searchResultContainer.repaint();
    }
    
    private void showSearchResultLoading(String query) {
        clearSearchResult();
        
        JLabel heading = new JLabel("Kết quả tìm kiếm");
        heading.setForeground(COLOR_TEXT_SECONDARY);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setBorder(BorderFactory.createEmptyBorder(12, 18, 5, 18));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchResultContainer.add(heading);
        
        JLabel loading = new JLabel("Đang tìm kiếm số: " + query + " ...");
        loading.setForeground(COLOR_TEXT_SECONDARY);
        loading.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        loading.setBorder(BorderFactory.createEmptyBorder(5, 18, 12, 18));
        loading.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchResultContainer.add(loading);
        
        searchResultContainer.revalidate();
        searchResultContainer.repaint();
    }
    
    private void showSearchResultMessage(String message, boolean highlight) {
        clearSearchResult();
        
        JLabel heading = new JLabel("Kết quả tìm kiếm");
        heading.setForeground(COLOR_TEXT_SECONDARY);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setBorder(BorderFactory.createEmptyBorder(12, 18, 5, 18));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchResultContainer.add(heading);
        
        JLabel label = new JLabel(message);
        label.setForeground(highlight ? COLOR_TEXT_PRIMARY : COLOR_TEXT_SECONDARY);
        label.setFont(new Font("Segoe UI", highlight ? Font.BOLD : Font.ITALIC, 13));
        label.setBorder(BorderFactory.createEmptyBorder(5, 18, 12, 18));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchResultContainer.add(label);
        
        searchResultContainer.revalidate();
        searchResultContainer.repaint();
    }
    
    private void showSearchResultUser(User user) {
        clearSearchResult();
        
        JLabel heading = new JLabel("Kết quả tìm kiếm");
        heading.setForeground(COLOR_TEXT_SECONDARY);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setBorder(BorderFactory.createEmptyBorder(12, 18, 8, 18));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchResultContainer.add(heading);
        
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(COLOR_ITEM_SELECTED);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(COLOR_ITEM_SELECTED.getRGB()), 1, true),
            BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JLabel nameLabel = new JLabel(user.getUsername());
        nameLabel.setForeground(COLOR_TEXT_PRIMARY);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        JLabel phoneLabel = new JLabel("SĐT: " + (user.getPhoneNumber() != null ? user.getPhoneNumber() : "Không có"));
        phoneLabel.setForeground(COLOR_TEXT_SECONDARY);
        phoneLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(phoneLabel);
        
        card.add(textPanel, BorderLayout.CENTER);
        
        if (onSendFriendRequest != null && user.getPhoneNumber() != null) {
            JButton addButton = new JButton("Kết bạn");
            addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addButton.setBackground(COLOR_ACCENT);
            addButton.setForeground(Color.WHITE);
            addButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
            addButton.setFocusPainted(false);
            addButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 1, true),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
            ));
            addButton.addActionListener(e -> {
                onSendFriendRequest.accept(user.getPhoneNumber());
                addButton.setEnabled(false);
                addButton.setText("Đã gửi");
                addButton.setBackground(COLOR_TEXT_SECONDARY);
            });
            addButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (addButton.isEnabled()) {
                        addButton.setBackground(COLOR_ACCENT.brighter());
                    }
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (addButton.isEnabled()) {
                        addButton.setBackground(COLOR_ACCENT);
                    }
                }
            });
            card.add(addButton, BorderLayout.EAST);
        }
        
        searchResultContainer.add(card);
        searchResultContainer.revalidate();
        searchResultContainer.repaint();
    }
    
    private boolean isPhoneNumberCandidate(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.replaceAll("[\\s\\-()]", "");
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return !normalized.isEmpty() && normalized.chars().allMatch(Character::isDigit) && normalized.length() >= 6;
    }
    
    public void showSearchResult(FriendSearchResult result) {
        if (result == null || searchField == null) {
            return;
        }
        
        String currentText = searchField.getText().trim();
        if (result.getQuery() == null || !result.getQuery().equals(currentText)) {
            return; // Bỏ qua nếu user đã nhập query khác
        }
        
        pendingSearchQuery = "";
        FriendSearchResult.Status status = result.getStatus() != null
            ? result.getStatus()
            : FriendSearchResult.Status.NOT_FOUND;
        
        switch (status) {
            case FOUND:
                if (result.getUser() != null) {
                    showSearchResultUser(result.getUser());
                } else {
                    showSearchResultMessage("Không thể hiển thị thông tin người dùng.", true);
                }
                break;
            case SELF:
                showSearchResultMessage("Đây là số điện thoại của bạn.", true);
                break;
            case ALREADY_CONNECTED:
                showSearchResultMessage("Bạn đã kết nối (hoặc đang chờ) với số điện thoại này.", true);
                break;
            case NOT_FOUND:
            default:
                showSearchResultMessage("Không tìm thấy người dùng với số: " + result.getQuery(), false);
                break;
        }
    }
    
    /**
     * Tạo một chat item cho mỗi user
     */
    
    
    private JPanel createChatItem(User friend) {
        JPanel item = new JPanel(new BorderLayout(15, 0));
        item.setBackground(COLOR_BACKGROUND);
        item.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        	
        String name = friend.getUsername();
        String userID = String.valueOf(friend.getId());
        
        // Avatar (chữ cái đầu) - Improved styling
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                   java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                   java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                
                // Vẽ hình tròn với shadow effect
                g2.setColor(new Color(COLOR_ACCENT.getRed(), COLOR_ACCENT.getGreen(), COLOR_ACCENT.getBlue(), 200));
                g2.fillOval(1, 1, 48, 48);
                g2.setColor(COLOR_ACCENT);
                g2.fillOval(0, 0, 48, 48);
                
                // Vẽ chữ cái
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                String initial = name.substring(0, 1).toUpperCase();
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = (48 - fm.stringWidth(initial)) / 2;
                int y = ((48 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
            }
        };
        avatarPanel.setPreferredSize(new Dimension(48, 48));
        avatarPanel.setOpaque(false);
        
        // Text panel
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(COLOR_TEXT_PRIMARY);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JLabel statusLabel = new JLabel(friend.isOnline() ? "● Online" : "○ Offline");
        statusLabel.setForeground(friend.isOnline() ? COLOR_ONLINE : COLOR_OFFLINE);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Lưu statusLabel để có thể cập nhật sau này
        statusLabels.put(userID, statusLabel);
        
        textPanel.add(nameLabel, BorderLayout.NORTH);
        textPanel.add(statusLabel, BorderLayout.CENTER);
        
        item.add(avatarPanel, BorderLayout.WEST);
        item.add(textPanel, BorderLayout.CENTER);
        
        // Mouse listeners với selection highlight
        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (item != currentSelectedItem) {
                    item.setBackground(COLOR_ITEM_HOVER);
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (item != currentSelectedItem) {
                    item.setBackground(COLOR_BACKGROUND);
                }
            }
            
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                // Bỏ highlight item cũ
                if (currentSelectedItem != null) {
                    currentSelectedItem.setBackground(COLOR_BACKGROUND);
                }
                
                // Highlight item mới
                currentSelectedItem = item;
                item.setBackground(COLOR_ITEM_SELECTED);
                
                // Gọi callback để mở chat
                if (onChatSelected != null) {
                    onChatSelected.accept(userID, name);
                }
            }
        });
        
        return item;
    }
    
    /**
     * Method để highlight một user hoặc conversation từ bên ngoài
     * Hỗ trợ cả userId (String) và conversationId (với prefix "convo_")
     */
    public void selectUser(String userId) {
        // Bỏ highlight cũ
        if (currentSelectedItem != null) {
            currentSelectedItem.setBackground(COLOR_BACKGROUND);
        }
        
        // Thử tìm với key trực tiếp (cho friend items)
        JPanel item = chatItemPanels.get(userId);
        
        // Nếu không tìm thấy, thử với prefix "convo_" (cho conversation items)
        if (item == null && !userId.startsWith("convo_")) {
            item = chatItemPanels.get("convo_" + userId);
        }
        
        if (item != null) {
            currentSelectedItem = item;
            item.setBackground(COLOR_ITEM_SELECTED);
        }
    }
    
    /**
     * Lấy User object từ ID
     */
    public User getUserById(int userId) {
        return userMap.get(userId);
    }
    
    /**
     * Cập nhật trạng thái online/offline của user
     * Tự động tìm và update status cho cả friend items và DIRECT conversation items
     */
    public void updateUserStatus(int userId, boolean isOnline) {
        // 1. Update cho friend items (nếu có trong userMap)
        User user = userMap.get(userId);
        String userIdStr = String.valueOf(userId);
        JLabel statusLabel = statusLabels.get(userIdStr);
        
        if (user != null) {
            user.setOnline(isOnline);
            
            // Cập nhật UI real-time cho friend items
            if (statusLabel != null) {
                statusLabel.setText(isOnline ? "● Online" : "○ Offline");
                statusLabel.setForeground(isOnline ? COLOR_ONLINE : COLOR_OFFLINE);
                statusLabel.repaint();
            }
        }
        
        // 2. Update cho DIRECT conversation items
        // Tìm tất cả conversations có userId này
        for (Map.Entry<Integer, Integer> entry : directConversationUserIdMap.entrySet()) {
            if (entry.getValue() == userId) {
                int conversationId = entry.getKey();
                String statusKey = "convo_" + conversationId;
                JLabel convoStatusLabel = statusLabels.get(statusKey);
                
                if (convoStatusLabel != null) {
                    convoStatusLabel.setText(isOnline ? "● Online" : "○ Offline");
                    convoStatusLabel.setForeground(isOnline ? COLOR_ONLINE : COLOR_OFFLINE);
                    convoStatusLabel.repaint();
                    
                    System.out.println("[ChatListPanel] Conversation " + conversationId 
                        + " (user " + userId + ") status updated to: "
                        + (isOnline ? "online" : "offline"));
                }
            }
        }
        
        System.out.println("[ChatListPanel] User " + userId + " status updated to: "
            + (isOnline ? "online" : "offline"));
    }
    
    public int getUserIdForConversation(int conversationId) {
        if (directConversationUserIdMap.containsKey(conversationId)) {
            return directConversationUserIdMap.get(conversationId);
        }
        return -1; // Không tìm thấy
    }
}