package View;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class WelcomePanel extends JPanel {
    
    public WelcomePanel() {
        // Dùng màu nền của ChatPanel
        setBackground(new Color(60, 63, 68)); 
        setLayout(new GridBagLayout()); // Dùng layout này để căn giữa dễ dàng

        JLabel welcomeLabel = new JLabel("Chọn một người để bắt đầu trò chuyện");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        welcomeLabel.setForeground(new Color(185, 187, 190)); // Màu text phụ

        // Thêm label vào giữa panel
        add(welcomeLabel, new GridBagConstraints());
    }
}