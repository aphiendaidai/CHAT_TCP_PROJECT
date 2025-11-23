package Controll;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.github.sarxos.webcam.Webcam;

public class VideoCallHandler {
    private DatagramSocket socket;
    private InetAddress targetIP;
    private int targetPort;
    private boolean isCalling = false;
    private Webcam webcam;
    private JLabel displayLabel; // Màn hình hiển thị video của đối phương

    public VideoCallHandler(JLabel displayLabel) {
        this.displayLabel = displayLabel;
        try {
            // Mở cổng ngẫu nhiên
            this.socket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public void setTarget(String ip, int port) {
        try {
            this.targetIP = InetAddress.getByName(ip);
            this.targetPort = port;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void startCall() {
        if (isCalling) return;
        isCalling = true;

        boolean cameraOpened = false;

        // 1. Cố gắng mở Webcam
        try {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                // Kiểm tra nếu webcam chưa mở thì mới mở
                if (!webcam.isOpen()) {
                    webcam.setViewSize(new Dimension(320, 240)); 
                    webcam.open();
                    cameraOpened = true;
                } else {
                    // Nếu đã mở rồi (do code khác dùng) thì coi như thành công
                    cameraOpened = true;
                }
            }
        } catch (Exception e) {
            System.err.println("[Video] Không thể mở Camera (Có thể đang bị App khác chiếm): " + e.getMessage());
            // KHÔNG RETURN Ở ĐÂY! Vẫn phải chạy tiếp để nhận hình ảnh từ bên kia.
        }

        // 2. Luôn luôn chạy luồng NHẬN (Để xem người kia, kể cả khi mình không có cam)
        new Thread(this::receiveAndShow).start();

        // 3. Chỉ chạy luồng GỬI nếu Camera mở thành công
        if (cameraOpened) {
            new Thread(this::captureAndSend).start();
        } else {
            System.out.println("[Video] Chế độ: Chỉ xem (No Camera Mode)");
        }
    }

    public void stopCall() {
        isCalling = false;
        
        // Đóng Webcam an toàn
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        
        // Đóng Socket
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("[Video] Đã dừng cuộc gọi video.");
    }

    // --- LUỒNG 1: QUAY PHIM -> NÉN -> GỬI ---
    private void captureAndSend() {
        try {
            while (isCalling && webcam != null && webcam.isOpen()) {
                // 1. Lấy ảnh từ Webcam
                BufferedImage image = webcam.getImage();
                
                if (image != null) {
                    // 2. Nén thành JPG
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    byte[] data = baos.toByteArray();

                    // 3. QUAN TRỌNG: Kiểm tra kích thước gói tin UDP
                    // Gói UDP tối đa 65507 bytes. An toàn là < 60000 bytes.
                    if (data.length < 60000) {
                        if (targetIP != null && targetPort > 0) {
                            DatagramPacket packet = new DatagramPacket(data, data.length, targetIP, targetPort);
                            if (!socket.isClosed()) {
                                socket.send(packet);
                            }
                        }
                    } else {
                        // Nếu ảnh quá lớn, ta bỏ qua frame này (giật 1 xíu còn hơn là sập)
                        // System.out.println("[Video] Frame quá lớn (" + data.length + " bytes) -> Bỏ qua");
                    }
                }
                
                // Giới hạn FPS khoảng 20-30 khung hình/giây để đỡ lag
                Thread.sleep(40); 
            }
        } catch (Exception e) {
            // Lỗi nhẹ thì bỏ qua, lỗi nặng in ra
            if (isCalling) e.printStackTrace();
        }
    }

    // --- LUỒNG 2: NHẬN -> GIẢI MÃ -> HIỂN THỊ ---
    private void receiveAndShow() {
        try {
            byte[] buffer = new byte[65507]; 
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("[Video] Đang lắng nghe video tại port: " + socket.getLocalPort());

            while (isCalling) {
                try {
                    // 1. Nhận gói tin
                    socket.receive(packet);
                    
                    // ✅ DEBUG: In ra xem có nhận được gì không
                    // (Nếu dòng này không chạy -> Lỗi mạng/Port/IP)
                    // System.out.println("[Video] Nhận gói tin: " + packet.getLength() + " bytes từ " + packet.getAddress());
                    
                    // 2. Biến byte[] thành Ảnh
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    BufferedImage receivedImage = ImageIO.read(bais);

                    // 3. Vẽ lên màn hình
                    if (receivedImage != null) {
                        if (displayLabel != null) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                if (displayLabel.getWidth() > 0) {
                                    java.awt.Image scaled = receivedImage.getScaledInstance(
                                        displayLabel.getWidth(), 
                                        displayLabel.getHeight(), 
                                        java.awt.Image.SCALE_FAST
                                    );
                                    displayLabel.setIcon(new ImageIcon(scaled));
                                } else {
                                    displayLabel.setIcon(new ImageIcon(receivedImage));
                                }
                                displayLabel.repaint();
                            });
                        } else {
                            System.err.println("[Video] Lỗi: displayLabel bị NULL, không có chỗ để vẽ!");
                        }
                    } else {
                        System.err.println("[Video] Lỗi: Không decode được ảnh (ImageIO trả về null)");
                    }
                } catch (SocketException se) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Helper set label nếu khởi tạo null lúc đầu
    public void setDisplayLabel(JLabel label) {
        this.displayLabel = label;
    }
}