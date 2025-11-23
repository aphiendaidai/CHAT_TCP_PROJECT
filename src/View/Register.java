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

public class Register extends JPanel {
    private JTextField usernameField;
    private JTextField phoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JButton registerButton;
    private JButton backToLoginButton;

    public Register() {
        setBackground(new Color(30, 30, 30));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        JLabel title = new JLabel("Tạo tài khoản");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(40));

        add(makeLabel("Số điện thoại:"));
        phoneField = makeTextField();
        add(phoneField);
        add(Box.createVerticalStrut(20));

        add(makeLabel("Tên đăng nhập:"));
        usernameField = makeTextField();
        add(usernameField);
        add(Box.createVerticalStrut(20));

        add(makeLabel("Mật khẩu:"));
        passwordField = makePasswordField();
        add(passwordField);
        add(Box.createVerticalStrut(20));

        add(makeLabel("Nhập lại mật khẩu:"));
        confirmField = makePasswordField();
        add(confirmField);
        add(Box.createVerticalStrut(30));

        registerButton = makeButton("Đăng ký", new Color(0, 153, 102));
        add(registerButton);
        add(Box.createVerticalStrut(25));

        backToLoginButton = new JButton("⬅ Quay lại đăng nhập");
        backToLoginButton.setForeground(new Color(0, 150, 255));
        backToLoginButton.setBackground(new Color(30, 30, 30));
        backToLoginButton.setFocusPainted(false);
        backToLoginButton.setBorderPainted(false);
        backToLoginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(backToLoginButton);
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

    // getter + listener
    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPhoneNumber() {
        return phoneField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword()).trim();
    }

    public String getConfirmPassword() {
        return new String(confirmField.getPassword()).trim();
    }

    public void addRegisterListener(ActionListener l) {
        registerButton.addActionListener(l);
    }

    public void addBackToLoginListener(ActionListener l) {
        backToLoginButton.addActionListener(l);
    }
}
