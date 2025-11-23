package Controll;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioCallHandler {
    private DatagramSocket socket;
    private boolean isCalling = false;
    private int targetPort = 9876; // Port UDP thỏa thuận
    private InetAddress targetIP;  // IP của người nghe

    // Cấu hình âm thanh (8kHz, 16bit, Mono - Chuẩn thoại tiết kiệm băng thông)
    private AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);

//    public AudioCallHandler(String targetIPStr) {
//        try {
//            this.socket = new DatagramSocket(); // Socket để gửi/nhận
//            this.targetIP = InetAddress.getByName(targetIPStr);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    public AudioCallHandler() {
        try {
            // Mở một cổng ngẫu nhiên bất kỳ (Ví dụ: 51234)
            // Không truyền tham số vào đây -> Java tự chọn cổng rảnh
            this.socket = new DatagramSocket(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void setTarget(String ipStr, int port) {
        try {
            this.targetIP = InetAddress.getByName(ipStr);
            this.targetPort = port;
            System.out.println("[Audio] Đã set mục tiêu: " + ipStr + ":" + port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    // Bắt đầu cuộc gọi (Chạy 2 luồng: 1 thu, 1 phát)
    public void startCall() {
        isCalling = true;
        new Thread(this::captureAndSend).start(); // Luồng Gửi (Mic -> UDP)
        new Thread(this::receiveAndPlay).start(); // Luồng Nhận (UDP -> Loa)
    }
    
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    // Dừng cuộc gọi
    public void stopCall() {
        isCalling = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

 // 🎤 Thu âm từ Mic và Gửi đi
//    private void captureAndSend() {
//        try {
//            TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
//            microphone.open(format);
//            microphone.start();
//
//            byte[] buffer = new byte[1024];
//            System.out.println("[Call] Bắt đầu thu âm...");
//
//            while (isCalling) {
//                int bytesRead = microphone.read(buffer, 0, buffer.length);
//                if (bytesRead > 0) {
//                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, targetIP, targetPort);
//                    
//                    // ✅ FIX: Kiểm tra socket trước khi gửi
//                    if (socket != null && !socket.isClosed()) {
//                        socket.send(packet);
//                    } else {
//                        break; // Thoát vòng lặp ngay nếu socket đã đóng
//                    }
//                }
//            }
//            microphone.close();
//            System.out.println("[Call] Đã dừng thu âm.");
//            
//        } catch (SocketException e) {
//            // Socket đóng là chuyện bình thường khi stopCall, không cần in lỗi đỏ lòm
//            System.out.println("[Call] Socket đã đóng (Kết thúc cuộc gọi).");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    
    private void captureAndSend() {
        try {
            TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
            microphone.start();

            byte[] buffer = new byte[1024];
            System.out.println("[Call] Bắt đầu thu âm...");

            while (isCalling) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    // ✅ FIX: Kiểm tra targetIP. Nếu chưa có thì KHÔNG ĐƯỢC GỬI, nhưng cũng KHÔNG ĐƯỢC BREAK.
                    // Chỉ cần bỏ qua vòng lặp này và đợi vòng sau.
                    if (targetIP != null && targetPort > 0) {
                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, targetIP, targetPort);
                        if (socket != null && !socket.isClosed()) {
                            socket.send(packet);
                        } else {
                            break; // Socket đóng thì mới thoát
                        }
                    } else {
                        // Nếu chưa có IP đích, ngủ 10ms để đỡ tốn CPU rồi check lại
                        Thread.sleep(10);
                    }
                }
            }
            microphone.close();
            System.out.println("[Call] Đã dừng thu âm.");
            
        } catch (SocketException e) {
            System.out.println("[Call] Socket đã đóng.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔊 Nhận từ UDP và Phát ra Loa
    private void receiveAndPlay() {
        try {
            // Lưu ý: Bên nhận cần bind port cố định, ví dụ 9876
            // Trong thực tế cần logic trao đổi port qua TCP trước
            // Ở đây ví dụ socket đã được mở ở constructor hoặc logic khác
            
            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (isCalling) {
                try {
                    socket.receive(packet);
                    speakers.write(packet.getData(), 0, packet.getLength());
                } catch (SocketException se) {
                    // Socket đóng khi stopCall()
                    break;
                }
            }
            speakers.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}