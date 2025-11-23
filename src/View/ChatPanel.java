package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import Controll.ClientView;
import Model.Message;
import Model.User;

public class ChatPanel extends JPanel {
    
    private int friendId;
    private String friendName;
    private int conversationId;
    private String conversationName;
    
    private JLabel headerLabel;
    private JPanel messageListPanel;
    private JScrollPane scrollPane;
    private JTextArea inputArea;
    private JButton sendButton;
    private JButton emojiButton;
    private Map<Integer, String> senderNameCache = new HashMap<>();
    private EmojiManager emojiManager;

    // Màu sắc Modern Dark Theme
    private final Color COLOR_BACKGROUND = new Color(42, 44, 50);
    private final Color COLOR_HEADER = new Color(90, 95, 105);
    private final Color COLOR_INPUT_BG = new Color(55, 58, 64);
    private final Color COLOR_ITEM_HOVER = new Color(66, 70, 77);
    private final Color COLOR_ACCENT = new Color(88, 101, 242);
    private final Color COLOR_TEXT_PRIMARY = new Color(255, 255, 255);
    private final Color COLOR_MESSAGE_SELF = new Color(88, 101, 242);
    private final Color COLOR_MESSAGE_OTHER = new Color(55, 58, 64);
    private final Color COLOR_DIVIDER = new Color(55, 58, 64);

    public ChatPanel(int conversationId, String conversationName) {
        this.conversationId = conversationId;
        this.conversationName = conversationName;
        this.emojiManager = EmojiManager.getInstance();

        setLayout(new BorderLayout());
        setBackground(COLOR_BACKGROUND);

        initComponents();

        ClientView.getInstance().requestHistory(conversationId);

        System.out.println("[ChatPanel] Requesting chat history for: " + conversationName);
    }
    
    private void initComponents() {
        // ============== HEADER ==============
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_HEADER);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_DIVIDER));
        headerPanel.setPreferredSize(new Dimension(0, 65));
        
        headerLabel = new JLabel(conversationName);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        headerLabel.setForeground(COLOR_TEXT_PRIMARY);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));

        JPanel callPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        callPanel.setOpaque(false);
        
        // Nút Audio Call với icon
        JButton audioCallBtn = new JButton();
        audioCallBtn.setIcon(loadScaledIcon("/resources/emoji/phone_call.png", 24, 24));
        audioCallBtn.setContentAreaFilled(false);
        audioCallBtn.setBorderPainted(false);
        audioCallBtn.setFocusPainted(false);
        audioCallBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        audioCallBtn.setToolTipText("Gọi thoại");
        audioCallBtn.addActionListener(e -> {
            ClientView.getInstance().initiateCall(friendId, "AUDIO"); 
            JOptionPane.showMessageDialog(ChatPanel.this, 
                    "Đang gọi cho " + conversationName + "...", 
                    "Đang gọi", JOptionPane.INFORMATION_MESSAGE);
        });

        // Nút Video Call với icon
        JButton videoCallBtn = new JButton();
        videoCallBtn.setIcon(loadScaledIcon("/resources/emoji/video_call.png", 24, 24));
        videoCallBtn.setContentAreaFilled(false);
        videoCallBtn.setBorderPainted(false);
        videoCallBtn.setFocusPainted(false);
        videoCallBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        videoCallBtn.setToolTipText("Gọi video");
        videoCallBtn.addActionListener(e -> {
            ClientView.getInstance().initiateCall(friendId, "VIDEO");
            
            Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                MainFrame mainFrame = (MainFrame) window;
                mainFrame.showInCallDialog(conversationName, friendId, true); 
            }
        });

        callPanel.add(audioCallBtn);
        callPanel.add(videoCallBtn);
        
        headerPanel.add(callPanel, BorderLayout.EAST);
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);
        
        // ============== MESSAGE AREA với background ==============
        messageListPanel = new JPanel() {
            private Image backgroundImage;
            
            {
                try {
                    backgroundImage = ImageIO.read(getClass().getResource("/resources/emoji/chat_background.png"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    // Vẽ background lặp lại (tiled)
                    int imgWidth = backgroundImage.getWidth(null);
                    int imgHeight = backgroundImage.getHeight(null);
                    for (int x = 0; x < getWidth(); x += imgWidth) {
                        for (int y = 0; y < getHeight(); y += imgHeight) {
                            g2d.drawImage(backgroundImage, x, y, imgWidth, imgHeight, null);
                        }
                    }
                }
            }
        };
        messageListPanel.setLayout(new BoxLayout(messageListPanel, BoxLayout.Y_AXIS));
        messageListPanel.setOpaque(false); // Để hiện background
        messageListPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        scrollPane = new JScrollPane(messageListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
        
        // ============== INPUT AREA ==============
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(COLOR_BACKGROUND);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        
        // Panel chứa các nút bên trái
        JPanel leftButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftButtonsPanel.setOpaque(false);
        
        // Nút + với popup menu (dùng icon)
        JButton attachButton = new JButton();
        attachButton.setIcon(loadScaledIcon("/resources/emoji/attach.png", 24, 24));
        attachButton.setBackground(COLOR_INPUT_BG);
        attachButton.setFocusPainted(false);
        attachButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        attachButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        attachButton.setToolTipText("Đính kèm file");
        
        // Tạo popup menu cho attachment
        JPopupMenu attachMenu = new JPopupMenu();
        attachMenu.setBackground(COLOR_INPUT_BG);
        attachMenu.setBorder(BorderFactory.createLineBorder(COLOR_DIVIDER, 1));
        
        JMenuItem imageMenuItem = new JMenuItem("  Gửi ảnh");
        imageMenuItem.setIcon(loadScaledIcon("/resources/emoji/image.png", 20, 20));
        imageMenuItem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        imageMenuItem.setBackground(COLOR_INPUT_BG);
        imageMenuItem.setForeground(COLOR_TEXT_PRIMARY);
        imageMenuItem.setOpaque(true);
        imageMenuItem.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        imageMenuItem.addActionListener(e -> selectAndSendImage());
        imageMenuItem.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                imageMenuItem.setBackground(COLOR_ITEM_HOVER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                imageMenuItem.setBackground(COLOR_INPUT_BG);
            }
        });
        
        JMenuItem fileMenuItem = new JMenuItem("  Gửi file");
        fileMenuItem.setIcon(loadScaledIcon("/resources/emoji/file.png", 20, 20));
        fileMenuItem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fileMenuItem.setBackground(COLOR_INPUT_BG);
        fileMenuItem.setForeground(COLOR_TEXT_PRIMARY);
        fileMenuItem.setOpaque(true);
        fileMenuItem.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        fileMenuItem.addActionListener(e -> selectAndSendFile());
        fileMenuItem.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                fileMenuItem.setBackground(COLOR_ITEM_HOVER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fileMenuItem.setBackground(COLOR_INPUT_BG);
            }
        });
        
        attachMenu.add(imageMenuItem);
        attachMenu.add(fileMenuItem);
        
        attachButton.addActionListener(e -> {
            attachMenu.show(attachButton, 0, -attachMenu.getPreferredSize().height);
        });
        
        attachButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                attachButton.setBackground(COLOR_ITEM_HOVER);
                attachButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_ITEM_HOVER, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                attachButton.setBackground(COLOR_INPUT_BG);
                attachButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        // ============== NÚT EMOJI ==============
        emojiButton = new JButton();
        emojiButton.setIcon(loadScaledIcon("/resources/emoji/emoji.png", 24, 24));
        emojiButton.setBackground(COLOR_INPUT_BG);
        emojiButton.setFocusPainted(false);
        emojiButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        emojiButton.setToolTipText("Chọn emoji");
        emojiButton.addActionListener(e -> showEmojiPicker());
        
        emojiButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                emojiButton.setBackground(COLOR_ITEM_HOVER);
                emojiButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_ITEM_HOVER, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                emojiButton.setBackground(COLOR_INPUT_BG);
                emojiButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        leftButtonsPanel.add(attachButton);
        leftButtonsPanel.add(emojiButton);
        
        // Text area
        inputArea = new JTextArea(2, 20);
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        inputArea.setBackground(COLOR_INPUT_BG);
        inputArea.setForeground(COLOR_TEXT_PRIMARY);
        inputArea.setCaretColor(COLOR_ACCENT);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });
        
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(null);
        inputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Send button với icon
        sendButton = new JButton("Gửi");
        sendButton.setIcon(loadScaledIcon("/resources/emoji/send.png", 18, 18));
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sendButton.setBackground(COLOR_ACCENT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_ACCENT, 1, true),
            BorderFactory.createEmptyBorder(12, 30, 12, 30)
        ));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendMessage());
        
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(COLOR_ACCENT.brighter());
                sendButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_ACCENT.brighter(), 1, true),
                    BorderFactory.createEmptyBorder(12, 30, 12, 30)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(COLOR_ACCENT);
                sendButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_ACCENT, 1, true),
                    BorderFactory.createEmptyBorder(12, 30, 12, 30)
                ));
            }
        });
        
        inputPanel.add(leftButtonsPanel, BorderLayout.WEST);
        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    
//    private void initComponents() {
//        // ============== HEADER ==============
//        JPanel headerPanel = new JPanel(new BorderLayout());
//        headerPanel.setBackground(COLOR_HEADER);
//        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_DIVIDER));
//        headerPanel.setPreferredSize(new Dimension(0, 65));
//        
//        headerLabel = new JLabel(conversationName);
//        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
//        headerLabel.setForeground(COLOR_TEXT_PRIMARY);
//        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));
//
//        JPanel callPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
//        callPanel.setOpaque(false);
//        JButton audioCallBtn = new JButton("📞");	
//        audioCallBtn.setFont(new Font("Segoe UI", Font.PLAIN, 18));
//        audioCallBtn.setContentAreaFilled(false);
//        audioCallBtn.setBorderPainted(false);
//        audioCallBtn.setForeground(Color.WHITE);
//        audioCallBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        audioCallBtn.addActionListener(e -> {
//            // Gọi hàm audio cũ
//            ClientView.getInstance().initiateCall(friendId, "AUDIO"); 
//            JOptionPane.showMessageDialog(ChatPanel.this, 
//                    "Đang gọi cho " + conversationName + "...", 
//                    "Đang gọi", JOptionPane.INFORMATION_MESSAGE);
//        });
//
//        // 2. Nút Gọi Video (Video Call) - MỚI
//        JButton videoCallBtn = new JButton("🎥");
//        videoCallBtn.setFont(new Font("Segoe UI", Font.PLAIN, 18));
//        videoCallBtn.setContentAreaFilled(false);
//        videoCallBtn.setBorderPainted(false);
//        videoCallBtn.setForeground(Color.WHITE);
//        videoCallBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        videoCallBtn.addActionListener(e -> {
//            // 1. Gửi yêu cầu lên Server
//            ClientView.getInstance().initiateCall(friendId, "VIDEO");
//            
//            // 2. ✅ MỚI: Tìm MainFrame và mở cửa sổ "Đang gọi..." cho chính mình
//            Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
//            if (window instanceof MainFrame) {
//                MainFrame mainFrame = (MainFrame) window;
//                // Hiện cửa sổ Video, ID = friendId
//                mainFrame.showInCallDialog(conversationName, friendId, true); 
//            }
//        });
//
//        callPanel.add(audioCallBtn);
//        callPanel.add(videoCallBtn);
//        
//        headerPanel.add(callPanel, BorderLayout.EAST);
//        headerPanel.add(headerLabel, BorderLayout.CENTER);
//        add(headerPanel, BorderLayout.NORTH);
//        
//        // ============== MESSAGE AREA ==============
//        messageListPanel = new JPanel();
//        messageListPanel.setLayout(new BoxLayout(messageListPanel, BoxLayout.Y_AXIS));
//        messageListPanel.setBackground(COLOR_BACKGROUND);
//        messageListPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
//        
//        scrollPane = new JScrollPane(messageListPanel);
//        scrollPane.setBorder(null);
//        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
//        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
//        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
//        scrollPane.getViewport().setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
//        add(scrollPane, BorderLayout.CENTER);
//        
//        // ============== INPUT AREA ==============
//        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
//        inputPanel.setBackground(COLOR_BACKGROUND);
//        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
//        
//        // Panel chứa các nút bên trái
//        JPanel leftButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
//        leftButtonsPanel.setOpaque(false);
//        
//        // Nút + với popup menu
//        JButton attachButton = new JButton("+");
//        attachButton.setFont(new Font("Segoe UI", Font.BOLD, 24));
//        attachButton.setBackground(COLOR_INPUT_BG);
//        attachButton.setForeground(COLOR_TEXT_PRIMARY);
//        attachButton.setFocusPainted(false);
//        attachButton.setBorder(BorderFactory.createCompoundBorder(
//            BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
//            BorderFactory.createEmptyBorder(8, 12, 8, 12)
//        ));
//        attachButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        
//        // Tạo popup menu cho attachment
//        JPopupMenu attachMenu = new JPopupMenu();
//        attachMenu.setBackground(COLOR_INPUT_BG);
//        attachMenu.setBorder(BorderFactory.createLineBorder(COLOR_DIVIDER, 1));
//        
//        JMenuItem imageMenuItem = new JMenuItem("🖼️  Gửi ảnh");
//        imageMenuItem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
//        imageMenuItem.setBackground(COLOR_INPUT_BG);
//        imageMenuItem.setForeground(COLOR_TEXT_PRIMARY);
//        imageMenuItem.setOpaque(true);
//        imageMenuItem.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
//        imageMenuItem.addActionListener(e -> selectAndSendImage());
//        imageMenuItem.addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseEntered(java.awt.event.MouseEvent evt) {
//                imageMenuItem.setBackground(COLOR_ITEM_HOVER);
//            }
//            @Override
//            public void mouseExited(java.awt.event.MouseEvent evt) {
//                imageMenuItem.setBackground(COLOR_INPUT_BG);
//            }
//        });
//        
//        JMenuItem fileMenuItem = new JMenuItem("📎  Gửi file");
//        fileMenuItem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
//        fileMenuItem.setBackground(COLOR_INPUT_BG);
//        fileMenuItem.setForeground(COLOR_TEXT_PRIMARY);
//        fileMenuItem.setOpaque(true);
//        fileMenuItem.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
//        fileMenuItem.addActionListener(e -> selectAndSendFile());
//        fileMenuItem.addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseEntered(java.awt.event.MouseEvent evt) {
//                fileMenuItem.setBackground(COLOR_ITEM_HOVER);
//            }
//            @Override
//            public void mouseExited(java.awt.event.MouseEvent evt) {
//                fileMenuItem.setBackground(COLOR_INPUT_BG);
//            }
//        });
//        
//        attachMenu.add(imageMenuItem);
//        attachMenu.add(fileMenuItem);
//        
//        attachButton.addActionListener(e -> {
//            attachMenu.show(attachButton, 0, -attachMenu.getPreferredSize().height);
//        });
//        
//        attachButton.addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseEntered(java.awt.event.MouseEvent evt) {
//                attachButton.setBackground(COLOR_ITEM_HOVER);
//                attachButton.setBorder(BorderFactory.createCompoundBorder(
//                    BorderFactory.createLineBorder(COLOR_ITEM_HOVER, 1, true),
//                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
//                ));
//            }
//            @Override
//            public void mouseExited(java.awt.event.MouseEvent evt) {
//                attachButton.setBackground(COLOR_INPUT_BG);
//                attachButton.setBorder(BorderFactory.createCompoundBorder(
//                    BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
//                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
//                ));
//            }
//        });
//        
//        // ============== NÚT EMOJI ==============
//        emojiButton = new JButton("😊");
//        emojiButton.setFont(new Font("Segoe UI", Font.PLAIN, 20));
//        emojiButton.setBackground(COLOR_INPUT_BG);
//        emojiButton.setForeground(COLOR_TEXT_PRIMARY);
//        emojiButton.setFocusPainted(false);
//        emojiButton.setBorder(BorderFactory.createCompoundBorder(
//            BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
//            BorderFactory.createEmptyBorder(8, 12, 8, 12)
//        ));
//        emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        emojiButton.addActionListener(e -> showEmojiPicker());
//        
//        emojiButton.addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseEntered(java.awt.event.MouseEvent evt) {
//                emojiButton.setBackground(COLOR_ITEM_HOVER);
//                emojiButton.setBorder(BorderFactory.createCompoundBorder(
//                    BorderFactory.createLineBorder(COLOR_ITEM_HOVER, 1, true),
//                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
//                ));
//            }
//            @Override
//            public void mouseExited(java.awt.event.MouseEvent evt) {
//                emojiButton.setBackground(COLOR_INPUT_BG);
//                emojiButton.setBorder(BorderFactory.createCompoundBorder(
//                    BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
//                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
//                ));
//            }
//        });
//        
//        leftButtonsPanel.add(attachButton);
//        leftButtonsPanel.add(emojiButton);
//        
//        // Text area
//        inputArea = new JTextArea(2, 20);
//        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
//        inputArea.setBackground(COLOR_INPUT_BG);
//        inputArea.setForeground(COLOR_TEXT_PRIMARY);
//        inputArea.setCaretColor(COLOR_ACCENT);
//        inputArea.setLineWrap(true);
//        inputArea.setWrapStyleWord(true);
//        inputArea.setBorder(BorderFactory.createCompoundBorder(
//            BorderFactory.createLineBorder(COLOR_INPUT_BG, 1, true),
//            BorderFactory.createEmptyBorder(12, 18, 12, 18)
//        ));
//        
//        inputArea.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyPressed(KeyEvent e) {
//                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
//                    e.consume();
//                    sendMessage();
//                }
//            }
//        });
//        
//        JScrollPane inputScroll = new JScrollPane(inputArea);
//        inputScroll.setBorder(null);
//        inputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
//        inputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//        
//        // Send button
//        sendButton = new JButton("Gửi");
//        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
//        sendButton.setBackground(COLOR_ACCENT);
//        sendButton.setForeground(Color.WHITE);
//        sendButton.setFocusPainted(false);
//        sendButton.setBorder(BorderFactory.createCompoundBorder(
//            BorderFactory.createLineBorder(COLOR_ACCENT, 1, true),
//            BorderFactory.createEmptyBorder(12, 30, 12, 30)
//        ));
//        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        sendButton.addActionListener(e -> sendMessage());
//        
//        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseEntered(java.awt.event.MouseEvent evt) {
//                sendButton.setBackground(COLOR_ACCENT.brighter());
//                sendButton.setBorder(BorderFactory.createCompoundBorder(
//                    BorderFactory.createLineBorder(COLOR_ACCENT.brighter(), 1, true),
//                    BorderFactory.createEmptyBorder(12, 30, 12, 30)
//                ));
//            }
//            @Override
//            public void mouseExited(java.awt.event.MouseEvent evt) {
//                sendButton.setBackground(COLOR_ACCENT);
//                sendButton.setBorder(BorderFactory.createCompoundBorder(
//                    BorderFactory.createLineBorder(COLOR_ACCENT, 1, true),
//                    BorderFactory.createEmptyBorder(12, 30, 12, 30)
//                ));
//            }
//        });
//        
//        inputPanel.add(leftButtonsPanel, BorderLayout.WEST);
//        inputPanel.add(inputScroll, BorderLayout.CENTER);
//        inputPanel.add(sendButton, BorderLayout.EAST);
//        add(inputPanel, BorderLayout.SOUTH);
//    }
    
    
    private ImageIcon loadScaledIcon(String path, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(path));
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImg);
        } catch (Exception e) {
            System.err.println("Không thể load icon: " + path);
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Hiển thị emoji picker
     */
    private void showEmojiPicker() {
        JPopupMenu emojiPopup = new JPopupMenu();
        emojiPopup.setBackground(COLOR_INPUT_BG);
        emojiPopup.setBorder(BorderFactory.createLineBorder(COLOR_DIVIDER, 1));
        
        JPanel emojiPanel = new JPanel(new GridLayout(0, 6, 5, 5));
        emojiPanel.setBackground(COLOR_INPUT_BG);
        emojiPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] emojiCodes = emojiManager.getAllEmojiCodes();
        
        int loadedCount = 0;
        for (String emojiCode : emojiCodes) {
            ImageIcon icon = emojiManager.getEmojiIcon(emojiCode);
            if (icon != null) {
                JButton emojiBtn = new JButton(icon);
                emojiBtn.setPreferredSize(new Dimension(40, 40));
                emojiBtn.setBackground(COLOR_INPUT_BG);
                emojiBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                emojiBtn.setFocusPainted(false);
                emojiBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                emojiBtn.setToolTipText(emojiCode);
                
                emojiBtn.addActionListener(e -> {
                    insertEmojiIntoInput(emojiCode);
                    emojiPopup.setVisible(false);
                });
                
                emojiBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        emojiBtn.setBackground(COLOR_ITEM_HOVER);
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        emojiBtn.setBackground(COLOR_INPUT_BG);
                    }
                });
                
                emojiPanel.add(emojiBtn);
                loadedCount++;
            } else {
                // Debug: Emoji không load được
                System.err.println("[ChatPanel] Failed to load emoji: " + emojiCode);
            }
        }
        
        System.out.println("[ChatPanel] Loaded " + loadedCount + "/" + emojiCodes.length + " emojis");
        
        // Nếu không có emoji nào load được, hiển thị thông báo
        if (loadedCount == 0) {
            JLabel errorLabel = new JLabel("⚠️ Không thể tải emoji. Kiểm tra thư mục resources/emoji/");
            errorLabel.setForeground(COLOR_TEXT_PRIMARY);
            errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            errorLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            emojiPanel.add(errorLabel);
        }
        
        JScrollPane emojiScroll = new JScrollPane(emojiPanel);
        emojiScroll.setBorder(null);
        emojiScroll.setPreferredSize(new Dimension(280, 200));
        emojiScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        emojiPopup.add(emojiScroll);
        emojiPopup.show(emojiButton, 0, -emojiPopup.getPreferredSize().height);
    }
    
    /**
     * Chèn emoji vào input area
     */
    private void insertEmojiIntoInput(String emojiCode) {
        int caretPos = inputArea.getCaretPosition();
        String currentText = inputArea.getText();
        String newText = currentText.substring(0, caretPos) + emojiCode + currentText.substring(caretPos);
        inputArea.setText(newText);
        inputArea.setCaretPosition(caretPos + emojiCode.length());
        inputArea.requestFocus();
    }
    
    /**
     * Chọn và gửi ảnh
     */
    private void selectAndSendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(
            new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif", "bmp")
        );
        fileChooser.setDialogTitle("Chọn ảnh để gửi");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File imageFile = fileChooser.getSelectedFile();
            
            long fileSize = imageFile.length();
            if (fileSize > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, 
                    "Ảnh quá lớn! Vui lòng chọn ảnh nhỏ hơn 5MB.",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String fileName = imageFile.getName();
                
                ClientView.getInstance().sendImageMessage(conversationId, base64Image, fileName);
                
                Message tempMessage = new Message();
                tempMessage.setContent("");
                tempMessage.setImageBase64(base64Image);
                tempMessage.setImageFileName(fileName);
                tempMessage.setHasImage(true);
                tempMessage.setSentAt(LocalDateTime.now());
                tempMessage.setSenderId(ClientView.getInstance().getCurrentUser().getId());
                addMessageToUI(tempMessage, true);
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Lỗi đọc file ảnh: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Chọn và gửi file
     */
    private void selectAndSendFile() {
        JFileChooser fileChooser = new JFileChooser();
        
        fileChooser.setFileFilter(
            new FileNameExtensionFilter("Documents (PDF, DOC, DOCX, TXT, XLS, XLSX, PPT, PPTX)", 
                "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx")
        );
        fileChooser.setDialogTitle("Chọn file để gửi");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            long fileSize = selectedFile.length();
            if (fileSize > 10 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, 
                    "File quá lớn! Vui lòng chọn file nhỏ hơn 10MB.",
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                String base64File = Base64.getEncoder().encodeToString(fileBytes);
                String fileName = selectedFile.getName();
                
                ClientView.getInstance().sendFileMessage(conversationId, base64File, fileName);
                
                Message tempMessage = new Message();
                tempMessage.setContent("[Đã gửi file: " + fileName + "]");
                tempMessage.setImageUrl(null);
                tempMessage.setImageFileName(fileName);
                tempMessage.setHasImage(true);
                tempMessage.setSentAt(LocalDateTime.now());
                tempMessage.setSenderId(ClientView.getInstance().getCurrentUser().getId());
                addMessageToUI(tempMessage, true);
                
                System.out.println("[ChatPanel] File sent: " + fileName + " (" + fileSize + " bytes)");
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Lỗi đọc file: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Gửi tin nhắn
     */
    private void sendMessage() {
        String content = inputArea.getText().trim();
        
        if (content.isEmpty()) {
            return;
        }
        
        Message tempMessage = new Message();
        tempMessage.setContent(content);
        tempMessage.setSentAt(LocalDateTime.now());
        tempMessage.setSenderId(ClientView.getInstance().getCurrentUser().getId());
        addMessageToUI(tempMessage, true);
        
        ClientView.getInstance().sendMessage(conversationId, content);
        
        inputArea.setText("");
        inputArea.requestFocus();
    }
    
    /**
     * Thêm tin nhắn vào UI với xử lý emoji
     */
    public void addMessageToUI(Message message, boolean isSelf) {
        LocalDateTime sentAt = message != null ? message.getSentAt() : null;
        
        JPanel messageWrapper = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 12, 2));
        messageWrapper.setOpaque(false);
        messageWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        JPanel messageBubble = new JPanel();
        messageBubble.setLayout(new BorderLayout(0, 8));
        messageBubble.setBackground(isSelf ? COLOR_MESSAGE_SELF : COLOR_MESSAGE_OTHER);
        messageBubble.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                isSelf ? new Color(COLOR_MESSAGE_SELF.getRGB()) : new Color(COLOR_MESSAGE_OTHER.getRGB()), 
                1, true
            ),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        messageBubble.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        // Hiển thị tên người gửi
        if (!isSelf && message != null && message.getSenderName() != null) {
            JLabel senderLabel = new JLabel(message.getSenderName());
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            senderLabel.setForeground(COLOR_ACCENT);
            senderLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            contentPanel.add(senderLabel, BorderLayout.NORTH);
        }
        
        // Nội dung message
        JPanel messageContentPanel = new JPanel(new BorderLayout());
        messageContentPanel.setOpaque(false);
        
        if (message != null && message.hasImage()) {
            String fileName = message.getImageFileName();
            boolean isImage = isImageFile(fileName);
            
            if (isImage) {
                displayImage(message, messageContentPanel);
            } else {
                displayFileAttachment(message, messageContentPanel);
            }
        } else {
            String content = message != null ? message.getContent() : "";
            
            if (content == null || content.trim().isEmpty()) {
                content = "(Không có nội dung)";
            }
            
            // --- BƯỚC 1: ESCAPE TEXT GỐC TRƯỚC (Để tránh lỗi nếu user gõ dấu < >) ---
            // Chúng ta dùng hàm escape cơ bản, bỏ qua logic check <img> cũ đi
            String safeContent = content.replace("&", "&amp;")
                                        .replace("<", "&lt;")
                                        .replace(">", "&gt;")
                                        .replace("\"", "&quot;");

            // --- BƯỚC 2: CHÈN EMOJI VÀO TEXT ĐÃ AN TOÀN ---
            // Lúc này safeContent không còn chứa tag HTML lạ, nên chèn <img> vào là an toàn
            String contentWithEmoji = emojiManager.replaceEmojiCodesWithHtml(safeContent);
            
            // --- BƯỚC 3: TẠO HTML WRAPPER ---
            // Lưu ý: Phải bắt đầu bằng <html> thì JLabel mới render được ảnh
            String htmlContent = "<html><body style='width: 300px; font-family: Segoe UI; font-size: 15px; color: " + 
                                 (isSelf ? "white" : "white") + ";'>" + 
                                 contentWithEmoji.replace("\n", "<br>") + 
                                 "</body></html>";

            JLabel messageLabel = new JLabel(htmlContent);
            // messageLabel.setFont(...); // Không cần set font ở đây nữa vì đã set trong HTML style
            // messageLabel.setForeground(...); // Không cần set color nữa vì đã set trong HTML style
            messageLabel.setVerticalAlignment(JLabel.TOP);
            
            messageContentPanel.add(messageLabel, BorderLayout.CENTER);
        }
        
        contentPanel.add(messageContentPanel, BorderLayout.CENTER);
        
        // Thời gian
        String timeStr = formatTime(sentAt);
        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(new Color(COLOR_TEXT_PRIMARY.getRed(), COLOR_TEXT_PRIMARY.getGreen(), 
                                         COLOR_TEXT_PRIMARY.getBlue(), 180));
        timeLabel.setHorizontalAlignment(isSelf ? JLabel.RIGHT : JLabel.LEFT);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        
        messageBubble.add(contentPanel, BorderLayout.CENTER);
        messageWrapper.add(messageBubble);
        messageListPanel.add(messageWrapper);
        messageListPanel.add(Box.createVerticalStrut(2));
        
        messageListPanel.revalidate();
        messageListPanel.repaint();
        javax.swing.SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(
                scrollPane.getVerticalScrollBar().getMaximum()
            );
        });
    }
    
    private String getSenderName(int senderId) {
        if (senderNameCache.containsKey(senderId)) {
            return senderNameCache.get(senderId);
        }
        
        User currentUser = ClientView.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() == senderId) {
            return "Bạn";
        }
        
        List<User> friends = ClientView.getInstance().getFriendList();
        if (friends != null) {
            for (User friend : friends) {
                if (friend.getId() == senderId) {
                    senderNameCache.put(senderId, friend.getUsername());
                    return friend.getUsername();
                }
            }
        }
        
        String fallbackName = "User " + senderId;
        senderNameCache.put(senderId, fallbackName);
        return fallbackName;
    }
    
    /**
     * Hiển thị ảnh trong message
     */
    
    /**
     * Hiển thị ảnh trong message (Phiên bản Debug & Fix lỗi)
     */
    private void displayImage(Message message, JPanel contentPanel) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                java.awt.image.BufferedImage bufferedImage = null;

                // 1️⃣ TRY BASE64 FIRST
                if (message.getImageBase64() != null && !message.getImageBase64().isEmpty()) {
                    try {
                        String cleanBase64 = message.getImageBase64().trim().replaceAll("\\s", "");
                        byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
                        
                        // ✅ DÙNG ImageIO.read() - CHỜ ĐỢI LOAD XONG
                        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes);
                        bufferedImage = javax.imageio.ImageIO.read(bis);
                        bis.close();
                        
                        System.out.println("✅ Base64 ảnh load thành công: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi decode Base64: " + e.getMessage());
                    }
                }
                
                // 2️⃣ TRY IMAGE URL IF BASE64 NOT FOUND
                if (bufferedImage == null && message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    try {
                        bufferedImage = javax.imageio.ImageIO.read(new java.net.URL(message.getImageUrl()));
                        System.out.println("✅ URL ảnh load thành công: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi load URL: " + e.getMessage());
                    }
                }

                // 3️⃣ HIỂN THỊ
                if (bufferedImage != null) {
                    // Resize an toàn
                    int maxWidth = 280;
                    int originalWidth = bufferedImage.getWidth();    // ✅ CHẮC CHẮN CÓ GIÁ TRỊ
                    int originalHeight = bufferedImage.getHeight();  // ✅ CHẮC CHẮN CÓ GIÁ TRỊ

                    int newWidth = originalWidth;
                    int newHeight = originalHeight;

                    if (newWidth > maxWidth) {
                        newHeight = (int)(newHeight * (maxWidth / (double)newWidth));
                        newWidth = maxWidth;
                    }

                    // Resize bằng ImageIO (chất lượng tốt hơn)
                    java.awt.image.BufferedImage resizedImage = new java.awt.image.BufferedImage(
                        newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_RGB
                    );
                    java.awt.Graphics2D g2d = resizedImage.createGraphics();
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                         java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(bufferedImage, 0, 0, newWidth, newHeight, null);
                    g2d.dispose();

                    // Tạo ImageIcon từ ảnh đã resize
                    ImageIcon resizedIcon = new ImageIcon(resizedImage);
                    JLabel imageLabel = new JLabel(resizedIcon);
                    imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

                    final java.awt.image.BufferedImage originalBuffered = bufferedImage;
                    imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent evt) {
                            showFullSizeImage(new ImageIcon(originalBuffered));
                        }
                    });

                    contentPanel.add(imageLabel, BorderLayout.CENTER);
                    System.out.println("✅ Ảnh hiển thị thành công");
                } else {
                    // Không có dữ liệu ảnh
                    JPanel errorPanel = new JPanel();
                    errorPanel.setOpaque(false);
                    errorPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    errorPanel.setPreferredSize(new Dimension(280, 150));
                    
                    JLabel errorLabel = new JLabel("<html><center>❌<br>Không thể tải ảnh<br>(Dữ liệu bị lỗi)</center></html>");
                    errorLabel.setForeground(Color.RED);
                    errorPanel.add(errorLabel);
                    contentPanel.add(errorPanel, BorderLayout.CENTER);
                    
                    System.err.println("❌ Không có dữ liệu ảnh để hiển thị");
                }

                contentPanel.revalidate();
                contentPanel.repaint();

            } catch (Exception e) {
                e.printStackTrace();
                JLabel errorLabel = new JLabel("⚠️ Lỗi: " + e.getClass().getSimpleName());
                errorLabel.setForeground(Color.RED);
                contentPanel.add(errorLabel, BorderLayout.CENTER);
            }
        });
    }
 // Class hỗ trợ vẽ ảnh trực tiếp, không dùng JLabel để tránh lỗi icon vỡ
    class ImagePanel extends JPanel {
        private java.awt.image.BufferedImage image;

        public ImagePanel(java.awt.image.BufferedImage img, int width, int height) {
            this.image = img;
            // Set kích thước cho panel bằng đúng kích thước ảnh đã resize
            setPreferredSize(new Dimension(width, height));
            setMaximumSize(new Dimension(width, height));
            setMinimumSize(new Dimension(width, height));
            setOpaque(false); // Để background trong suốt
            
            // Biến con trỏ thành hình bàn tay
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                // Vẽ ảnh chất lượng cao (smooth)
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }
//    private void displayImage(Message message, JPanel contentPanel) {
//        try {
//            ImageIcon imageIcon = null;
//            
//            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
//                try {
//                    java.net.URI uri = new java.net.URI(message.getImageUrl());
//                    java.net.URL url = uri.toURL();
//                    try (java.io.InputStream is = url.openStream()) {
//                        byte[] imageBytes = is.readAllBytes();
//                        imageIcon = new ImageIcon(imageBytes);
//                    }
//                } catch (Exception urlEx) {
//                    @SuppressWarnings("deprecation")
//                    java.net.URL url = new java.net.URL(message.getImageUrl());
//                    imageIcon = new ImageIcon(url);
//                }
//            }
////            else if (message.getImageBase64() != null && !message.getImageBase64().isEmpty()) {
////                byte[] imageBytes = Base64.getDecoder().decode(message.getImageBase64());
////                imageIcon = new ImageIcon(imageBytes);
////            }
//         // Tìm đoạn này trong hàm displayImage của ChatPanel.java
//            else if (message.getImageBase64() != null && !message.getImageBase64().isEmpty()) {
//                try {
//                    byte[] imageBytes = Base64.getDecoder().decode(message.getImageBase64());
//                    
//                    // --- SỬA ĐỔI: Dùng ImageIO để đọc ảnh đồng bộ ---
//                    // ImageIO.read sẽ đợi cho đến khi ảnh được giải mã hoàn toàn
//                    // Điều này đảm bảo getWidth/getHeight không bao giờ trả về -1
//                    java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes);
//                    java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(bis);
//                    
//                    if (bufferedImage != null) {
//                        imageIcon = new ImageIcon(bufferedImage);
//                    }
//                    bis.close();
//                    // -----------------------------------------------
//                    
//                } catch (Exception e) {
//                    System.err.println("Lỗi decode Base64 image: " + e.getMessage());
//                }
//            }
//            
//            final ImageIcon finalImageIcon = imageIcon;
//            
//            if (imageIcon != null) {
//                Image originalImage = imageIcon.getImage();
//                
//                int maxWidth = 300;
//                int originalWidth = originalImage.getWidth(null);
//                int originalHeight = originalImage.getHeight(null);
//                
//                ImageIcon resizedIcon;
//                if (originalWidth > maxWidth) {
//                    int newHeight = (int)(originalHeight * (maxWidth / (double)originalWidth));
//                    Image resizedImage = originalImage.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
//                    resizedIcon = new ImageIcon(resizedImage);
//                } else {
//                    resizedIcon = imageIcon;
//                }
//                
//                JLabel imageLabel = new JLabel(resizedIcon);
//                imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
//                
//                imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
//                    @Override
//                    public void mouseClicked(java.awt.event.MouseEvent evt) {
//                        if (finalImageIcon != null) {
//                            showFullSizeImage(finalImageIcon);
//                        }
//                    }
//                });
//                
//                contentPanel.add(imageLabel, BorderLayout.CENTER);
//            } else {
//                JLabel errorLabel = new JLabel("⚠️ Không thể hiển thị ảnh");
//                errorLabel.setForeground(COLOR_TEXT_PRIMARY);
//                contentPanel.add(errorLabel, BorderLayout.CENTER);
//            }
//            
//        } catch (Exception e) {
//            System.err.println("[ChatPanel] Error displaying image: " + e.getMessage());
//            JLabel errorLabel = new JLabel("⚠️ Lỗi hiển thị ảnh");
//            errorLabel.setForeground(COLOR_TEXT_PRIMARY);
//            contentPanel.add(errorLabel, BorderLayout.CENTER);
//        }
//    }
//    
    /**
     * Hiển thị file attachment với icon và nút download
     */
    private void displayFileAttachment(Message message, JPanel contentPanel) {
        String fileName = message.getImageFileName();
        String fileUrl = message.getImageUrl();
        
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePanel.setOpaque(false);
        
        JLabel fileIcon = new JLabel(getFileIcon(fileName) + "  " + fileName);
        fileIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fileIcon.setForeground(COLOR_TEXT_PRIMARY);
        
        filePanel.add(fileIcon);
        
        if (fileUrl != null && !fileUrl.isEmpty()) {
            JButton downloadButton = new JButton("⬇️ Tải xuống");
            downloadButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            downloadButton.setBackground(COLOR_ACCENT);
            downloadButton.setForeground(Color.WHITE);
            downloadButton.setFocusPainted(false);
            downloadButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            downloadButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            downloadButton.setAlignmentX(LEFT_ALIGNMENT);
            
            downloadButton.addActionListener(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(fileUrl));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Không thể mở link: " + ex.getMessage());
                }
            });
            
            filePanel.add(Box.createVerticalStrut(8));
            filePanel.add(downloadButton);
        }
        
        contentPanel.add(filePanel, BorderLayout.CENTER);
    }
    
    /**
     * Lấy icon phù hợp với loại file
     */
    private String getFileIcon(String fileName) {
        if (fileName == null) return "📄";
        
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "📕";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "📘";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "📗";
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "📙";
        if (lower.endsWith(".txt")) return "📝";
        if (lower.endsWith(".zip") || lower.endsWith(".rar")) return "🗜️";
        return "📄";
    }
    
    /**
     * Kiểm tra file có phải là ảnh không
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
               lower.endsWith(".png") || lower.endsWith(".gif") || 
               lower.endsWith(".bmp") || lower.endsWith(".webp");
    }
    
    /**
     * Hiển thị ảnh full size
     */
    private void showFullSizeImage(ImageIcon imageIcon) {
        JFrame imageFrame = new JFrame("Xem ảnh");
        JLabel fullImageLabel = new JLabel(imageIcon);
        imageFrame.add(fullImageLabel);
        imageFrame.pack();
        imageFrame.setLocationRelativeTo(this);
        imageFrame.setVisible(true);
    }
    
    /**
     * Escape HTML - cần tránh escape các emoji đã được convert
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        
        // Nếu text đã chứa img tag (emoji đã được convert), không escape
        if (text.contains("<img src='data:image/png;base64,")) {
            return text;
        }
        
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * @deprecated Use addMessageToUI(Message, boolean)
     */
    @Deprecated
    public void addMessageToUI(String content, boolean isSelf) {
        Message tempMessage = new Message();
        tempMessage.setContent(content);
        tempMessage.setSentAt(LocalDateTime.now());
        addMessageToUI(tempMessage, isSelf);
    }
    
    /**
     * Format thời gian
     */
    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return dateTime.format(timeFormatter);
        } else {
            return dateTime.format(dateFormatter) + " " + dateTime.format(timeFormatter);
        }
    }
    
    public void setHeaderTitle(String title) {
        this.friendName = title;
        if (headerLabel != null) {
            headerLabel.setText(title);
        }
    }
    
    public void clearMessages() {
        messageListPanel.removeAll();
        messageListPanel.revalidate();
        messageListPanel.repaint();
    }
    
    public int getFriendId() {
        return friendId;
    }
 // Trong ChatPanel.java

    public void setFriendId(int friendId) {
        this.friendId = friendId;
    }
    
}