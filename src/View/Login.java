package View;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class Login extends JPanel {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton goToRegisterButton;

    public Login() {
        setBackground(new Color(30, 30, 30));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        JLabel title = new JLabel("Zalo Chat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(title);

        add(Box.createVerticalStrut(40));

        add(makeLabel("Tên đăng nhập:"));
        usernameField = makeTextField();
        add(usernameField);

        add(Box.createVerticalStrut(20));

        add(makeLabel("Mật khẩu:"));
        passwordField = makePasswordField();
        add(passwordField);

        add(Box.createVerticalStrut(30));

        loginButton = makeButton("Đăng nhập", new Color(0, 120, 215));
        add(loginButton);

        add(Box.createVerticalStrut(25));

        goToRegisterButton = new JButton("Chưa có tài khoản? Đăng ký ngay");
        goToRegisterButton.setForeground(new Color(0, 150, 255));
        goToRegisterButton.setBackground(new Color(30, 30, 30));
        goToRegisterButton.setFocusPainted(false);
        goToRegisterButton.setBorderPainted(false);
        goToRegisterButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(goToRegisterButton);
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.LIGHT_GRAY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField makeTextField() {
        JTextField f = new JTextField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        f.setBackground(new Color(45, 45, 45));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return f;
    }

    private JPasswordField makePasswordField() {
        JPasswordField f = new JPasswordField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        f.setBackground(new Color(45, 45, 45));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return f;
    }

    private JButton makeButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(200, 40));
        b.setMaximumSize(new Dimension(200, 40));
        return b;
    }

    // getters
    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword()).trim();
    }

    public void addLoginListener(ActionListener l) {
        loginButton.addActionListener(l);
        passwordField.addActionListener(l);
    }

    public void addGoToRegisterListener(ActionListener l) {
        goToRegisterButton.addActionListener(l);
    }
}
