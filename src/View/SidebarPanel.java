package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import Controll.ClientView;
import Model.User;

public class SidebarPanel extends JPanel {

    private final Color COLOR_BACKGROUND = new Color(40, 42, 48);
    private final Color COLOR_HOVER_BG = new Color(55, 58, 64);
    private final Color COLOR_ACCENT = new Color(88, 101, 242);
    private final Color COLOR_ICON_SECONDARY = new Color(142, 146, 151);
    private final Color COLOR_ICON_PRIMARY = Color.WHITE;
    private final Color COLOR_SUCCESS = new Color(67, 181, 129);
    private final Color COLOR_DANGER = new Color(237, 66, 69);
    private final Color COLOR_TEXT_PRIMARY = new Color(255, 255, 255);
    private final Color COLOR_TEXT_SECONDARY = new Color(170, 173, 178);
    private final Color COLOR_DIVIDER = new Color(55, 58, 64);

    private JPanel requestPanel;
    private JScrollPane requestScrollPane;
    private JLabel nameLabel;
    private JLabel reqLabel;
    private AvatarIcon profilePic;

    private static final int SIDEBAR_WIDTH = 280;
    
    private Runnable onCreateGroupCallback;

    public SidebarPanel(User user) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 700));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_DIVIDER));
        setOpaque(false);

        // ============== HEADER SECTION ==============
        JPanel headerPanel = createHeaderPanel(user);
        add(headerPanel, BorderLayout.NORTH);

        // ============== REQUESTS SECTION ==============
        JPanel requestsSection = createRequestsSection();
        add(requestsSection, BorderLayout.CENTER);

        // ============== FOOTER SECTION ==============
        JPanel footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Vẽ background
        try {
            Image bgImage = ImageIO.read(getClass().getResource("/resources/emoji/chat_background_2.png"));
            if (bgImage != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // Vẽ background lặp lại (tiled)
                int imgWidth = bgImage.getWidth(null);
                int imgHeight = bgImage.getHeight(null);
                for (int x = 0; x < getWidth(); x += imgWidth) {
                    for (int y = 0; y < getHeight(); y += imgHeight) {
                        g2d.drawImage(bgImage, x, y, imgWidth, imgHeight, null);
                    }
                }
                
                // Thêm lớp phủ tối (overlay) - điều chỉnh alpha để tối hơn/sáng hơn
                // Alpha: 0-255 (0 = trong suốt, 255 = đậm hoàn toàn)
                // Tăng số này lên để tối hơn: 100, 120, 150, 180...
                g2d.setColor(new Color(0, 0, 0, 80)); // Màu đen với độ trong suốt
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        } catch (Exception e) {
            // Nếu không load được background thì dùng màu mặc định
            g.setColor(COLOR_BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    
    public void setCreateGroupCallback(Runnable callback) {
        this.onCreateGroupCallback = callback;
    }
    
    private JPanel createHeaderPanel(User user) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(30, 25, 25, 25));

        // Avatar
        String firstLetter = user.getUsername().substring(0, 1).toUpperCase();
        profilePic = new AvatarIcon(firstLetter, COLOR_ACCENT, COLOR_ICON_PRIMARY, 70);
        profilePic.setAlignmentX(Component.CENTER_ALIGNMENT);
        profilePic.setCursor(new Cursor(Cursor.HAND_CURSOR));
        header.add(profilePic);

        header.add(Box.createVerticalStrut(18));

        // Tên người dùng
        nameLabel = new JLabel(user.getUsername());
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setForeground(COLOR_TEXT_PRIMARY);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        header.add(nameLabel);

        // Divider
        header.add(Box.createVerticalStrut(25));
        JPanel divider = new JPanel();
        divider.setBackground(COLOR_DIVIDER);
        divider.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        header.add(divider);

        return header;
    }

    private JPanel createRequestsSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));

        // Tiêu đề
        reqLabel = new JLabel("Lời mời kết bạn");
        reqLabel.setForeground(COLOR_TEXT_SECONDARY);
        reqLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        reqLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        section.add(reqLabel, BorderLayout.NORTH);

        // Panel chứa danh sách lời mời (có scroll)
        requestPanel = new JPanel();
        requestPanel.setLayout(new BoxLayout(requestPanel, BoxLayout.Y_AXIS));
        requestPanel.setOpaque(false);

        requestScrollPane = new JScrollPane(requestPanel);
        requestScrollPane.setBorder(null);
        requestScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        requestScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        requestScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        requestScrollPane.setOpaque(false);
        requestScrollPane.getViewport().setOpaque(false);

        section.add(requestScrollPane, BorderLayout.CENTER);

        return section;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(18, 25, 25, 25));

        // Divider
        JPanel divider = new JPanel();
        divider.setBackground(COLOR_DIVIDER);
        divider.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        footer.add(divider);

        footer.add(Box.createVerticalStrut(18));

        JPanel buttonRowPanel = new JPanel();
        buttonRowPanel.setLayout(new BoxLayout(buttonRowPanel, BoxLayout.X_AXIS));
        buttonRowPanel.setOpaque(false);
        buttonRowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Nút Cài đặt với icon
        JButton settingsBtn = createIconButton("/resources/emoji/settings.png",  "Cài đặt");
        
        // Nút Tạo Nhóm với icon
        JButton createGroupBtn = createIconButton("/resources/emoji/add_group.png", "Tạo nhóm");
        createGroupBtn.addActionListener(e -> {
            if (onCreateGroupCallback != null) {
                onCreateGroupCallback.run();
            }
        });
        
        buttonRowPanel.add(settingsBtn);
        buttonRowPanel.add(Box.createHorizontalStrut(15));
        buttonRowPanel.add(createGroupBtn);

        footer.add(buttonRowPanel);

        return footer;
    }

    // ============================================================
    // ================ CẬP NHẬT DANH SÁCH LỜI MỜI ================
    // ============================================================

    public void updatePendingRequests(List<User> pendingRequests) {
        requestPanel.removeAll();

        if (pendingRequests == null || pendingRequests.isEmpty()) {
            JLabel none = new JLabel("Không có lời mời nào");
            none.setForeground(COLOR_TEXT_SECONDARY);
            none.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            none.setAlignmentX(Component.CENTER_ALIGNMENT);
            none.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
            requestPanel.add(none);
        } else {
            for (User u : pendingRequests) {
                JPanel row = createRequestRow(u);
                requestPanel.add(row);
                requestPanel.add(Box.createVerticalStrut(12));
            }
        }

        requestPanel.revalidate();
        requestPanel.repaint();
    }

    // ✅ Tạo row với avatar và tên đầy đủ
    private JPanel createRequestRow(User u) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_DIVIDER, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Avatar nhỏ
        String initial = u.getUsername().substring(0, 1).toUpperCase();
        AvatarIcon miniAvatar = new AvatarIcon(initial, COLOR_ACCENT, COLOR_ICON_PRIMARY, 40);

        // Panel chứa tên và nút
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Tên người gửi (hiển thị đầy đủ)
        JLabel nameLabel = new JLabel(u.getUsername());
        nameLabel.setForeground(COLOR_TEXT_PRIMARY);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(nameLabel);
        
        contentPanel.add(Box.createVerticalStrut(6));

        // Panel chứa 2 nút icon
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Nút Chấp nhận với icon
        JButton acceptBtn = createSmallIconButton("/resources/emoji/accept.png", COLOR_SUCCESS);
        acceptBtn.addActionListener(e -> {
            ClientView.getInstance().acceptFriendRequest(u.getId());
            removeRequestRow(row);
            ClientView.getInstance().requestFriendList();
            ClientView.getInstance().requestPendingFriendRequests();
        });

        // Nút Từ chối với icon
        JButton declineBtn = createSmallIconButton("/resources/emoji/decline.png", COLOR_DANGER);
        declineBtn.addActionListener(e -> {
            // TODO: Gọi API từ chối
            removeRequestRow(row);
        });

        btnPanel.add(acceptBtn);
        btnPanel.add(Box.createHorizontalStrut(8));
        btnPanel.add(declineBtn);

        contentPanel.add(btnPanel);

        row.add(miniAvatar, BorderLayout.WEST);
        row.add(contentPanel, BorderLayout.CENTER);

        // Hover effect
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                row.setBackground(COLOR_HOVER_BG);
                row.setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                row.setOpaque(false);
            }
        });

        return row;
    }

    private void removeRequestRow(JPanel row) {
        requestPanel.remove(row);
        requestPanel.revalidate();
        requestPanel.repaint();
    }

    // ✅ Tạo nút nhỏ với icon cho Accept/Decline
    private JButton createSmallIconButton(String iconPath, Color bgColor) {
        JButton btn = new JButton();
        btn.setIcon(loadScaledIcon(iconPath, 20, 20));
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setMinimumSize(new Dimension(32, 32));
        btn.setMaximumSize(new Dimension(32, 32));
        btn.setBackground(bgColor);
        btn.setBorder(BorderFactory.createLineBorder(bgColor, 1, true));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.brighter());
                btn.setBorder(BorderFactory.createLineBorder(bgColor.brighter(), 1, true));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
                btn.setBorder(BorderFactory.createLineBorder(bgColor, 1, true));
            }
        });

        return btn;
    }

    // ============================================================
    // ====================== TẠO NÚT ICON ========================
    // ============================================================

    private JButton createIconButton(String iconPath, String tooltipText) {
        JButton btn = new JButton();
        btn.setIcon(loadScaledIcon(iconPath, 26, 26));
        btn.setPreferredSize(new Dimension(50, 50));
        btn.setMaximumSize(new Dimension(50, 50));
        btn.setMinimumSize(new Dimension(50, 50));
        btn.setBackground(COLOR_BACKGROUND);
        btn.setBorder(BorderFactory.createLineBorder(COLOR_DIVIDER, 1, true));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tooltipText);

        btn.addMouseListener(new MouseAdapter() {
            private ImageIcon normalIcon = loadScaledIcon(iconPath, 26, 26);
            private ImageIcon hoverIcon = loadScaledIcon(iconPath.replace(".png", "_hover.png"), 26, 26);
            
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(COLOR_HOVER_BG);
                btn.setBorder(BorderFactory.createLineBorder(COLOR_ACCENT, 1, true));
                // Đổi sang icon sáng hơn khi hover
                if (hoverIcon != null && hoverIcon.getIconWidth() > 0) {
                    btn.setIcon(hoverIcon);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(COLOR_BACKGROUND);
                btn.setBorder(BorderFactory.createLineBorder(COLOR_DIVIDER, 1, true));
                // Đổi lại icon bình thường
                btn.setIcon(normalIcon);
            }
        });

        return btn;
    }

    // Helper method để load và scale icon
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

    // ============================================================
    // ========================= ICON CLASSES =====================
    // ============================================================

    private class AvatarIcon extends JComponent {
        private String letter;
        private Color bgColor;
        private Color fgColor;
        private int size;

        public AvatarIcon(String letter, Color bgColor, Color fgColor, int size) {
            this.letter = letter;
            this.bgColor = bgColor;
            this.fgColor = fgColor;
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Shadow effect
            g2d.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 150));
            g2d.fillOval(1, 1, size, size);
            
            g2d.setColor(bgColor);
            g2d.fillOval(0, 0, size, size);

            g2d.setColor(fgColor);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, size / 2 + 2));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (size - fm.stringWidth(letter)) / 2;
            int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(letter, x, y);

            g2d.dispose();
        }
    }
}