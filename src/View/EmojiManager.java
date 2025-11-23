package View;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * Quản lý emoji icons từ resources
 */
public class EmojiManager {
    private static EmojiManager instance;
    private Map<String, ImageIcon> emojiCache = new HashMap<>();
    private static final int EMOJI_SIZE = 32; // Kích thước emoji hiển thị
    
    // Mapping giữa emoji code và file name
    private static final Map<String, String> EMOJI_MAPPING = new HashMap<>();
    
    static {
        // Smileys & People
        EMOJI_MAPPING.put(":smile:", "smile.png");
        EMOJI_MAPPING.put(":laugh:", "laugh.png");
        EMOJI_MAPPING.put(":happy:", "happy.png");
        EMOJI_MAPPING.put(":wink:", "wink.png");
        EMOJI_MAPPING.put(":love-eyes:", "love-eyes.png");
        EMOJI_MAPPING.put(":kiss:", "kiss.png");
        EMOJI_MAPPING.put(":sad:", "sad.png");
        EMOJI_MAPPING.put(":cry:", "cry.png");
        EMOJI_MAPPING.put(":angry:", "angry.png");
        EMOJI_MAPPING.put(":cool:", "cool.png");	
        EMOJI_MAPPING.put(":thinking:", "thinking.png");
        EMOJI_MAPPING.put(":sleep:", "sleep.png");
        
        // Hearts & Emotions
        EMOJI_MAPPING.put(":heart:", "heart.png");
        EMOJI_MAPPING.put(":heart-red:", "heart-red.png");
        EMOJI_MAPPING.put(":heart-orange:", "heart-orange.png");
        EMOJI_MAPPING.put(":heart-yellow:", "heart-yellow.png");
        EMOJI_MAPPING.put(":heart-green:", "heart-green.png");
        EMOJI_MAPPING.put(":heart-blue:", "heart-blue.png");
        EMOJI_MAPPING.put(":heart-purple:", "heart-purple.png");
        EMOJI_MAPPING.put(":heart-broken:", "heart-broken.png");
        
        // Gestures	
        EMOJI_MAPPING.put(":thumbsup:", "thumbsup.png");
        EMOJI_MAPPING.put(":thumbsdown:", "thumbsdown.png");
        EMOJI_MAPPING.put(":ok:", "ok.png");
        EMOJI_MAPPING.put(":clap:", "clap.png");
        EMOJI_MAPPING.put(":wave:", "wave.png");
        EMOJI_MAPPING.put(":pray:", "pray.png");	
        
//        // Food
//        EMOJI_MAPPING.put(":pizza:", "pizza.png");
//        EMOJI_MAPPING.put(":burger:", "burger.png");
//        EMOJI_MAPPING.put(":coffee:", "coffee.png");
//        EMOJI_MAPPING.put(":beer:", "beer.png");
//        EMOJI_MAPPING.put(":cake:", "cake.png");
//        
//        // Objects & Symbols
//        EMOJI_MAPPING.put(":fire:", "fire.png");
//        EMOJI_MAPPING.put(":star:", "star.png");
//        EMOJI_MAPPING.put(":check:", "check.png");
//        EMOJI_MAPPING.put(":cross:", "cross.png");
//        EMOJI_MAPPING.put(":exclamation:", "exclamation.png");
//        EMOJI_MAPPING.put(":question:", "question.png");
    }
    
    private EmojiManager() {
        // ✅ PRELOAD TẤT CẢ EMOJI KHI KHỞI TẠO
        preloadAllEmojis();
    }
    
    /**
     * Preload tất cả emoji từ resources khi EmojiManager được tạo
     * ✅ GIẢI PHÁP CHO LỖI ICON VỠ
     */
    private void preloadAllEmojis() {
        System.out.println("[EmojiManager] 🔄 Preloading emojis...");
        int successCount = 0;
        int failCount = 0;
        
        for (Map.Entry<String, String> entry : EMOJI_MAPPING.entrySet()) {
            String emojiCode = entry.getKey();
            String fileName = entry.getValue();
            
            try {
                // Tìm resource
                InputStream is = getClass().getResourceAsStream("/resources/emoji/" + fileName);
                if (is == null) {
                    is = getClass().getResourceAsStream("resources/emoji/" + fileName);
                }
                
                if (is != null) {
                    // ✅ Đọc byte array
                    byte[] imageBytes = is.readAllBytes();
                    is.close();
                    
                    // ✅ Dùng BufferedImage để tránh async loading
                    java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes);
                    java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(bis);
                    bis.close();
                    
                    if (bufferedImage != null) {
                        // ✅ Resize ảnh
                        java.awt.image.BufferedImage resizedImage = 
                            new java.awt.image.BufferedImage(EMOJI_SIZE, EMOJI_SIZE, 
                                                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g2d = resizedImage.createGraphics();
                        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                           java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(bufferedImage, 0, 0, EMOJI_SIZE, EMOJI_SIZE, null);
                        g2d.dispose();
                        
                        // ✅ Cache ImageIcon từ BufferedImage (không async)
                        ImageIcon icon = new ImageIcon(resizedImage);
                        emojiCache.put(emojiCode, icon);
                        successCount++;
                    }
                } else {
                    failCount++;
                    System.err.println("[EmojiManager] ❌ File not found: " + fileName);
                }
                
            } catch (Exception e) {
                failCount++;
                System.err.println("[EmojiManager] ❌ Error loading " + emojiCode + ": " + e.getMessage());
            }
        }
        
        System.out.println("[EmojiManager] ✅ Preload complete: " + successCount + " loaded, " + failCount + " failed");
    }
    
    public static EmojiManager getInstance() {
        if (instance == null) {
            instance = new EmojiManager();
        }
        return instance;
    }
    
    /**
     * Lấy ImageIcon của emoji từ code
     * @param emojiCode code emoji (ví dụ: ":smile:")
     * @return ImageIcon hoặc null nếu không tìm thấy
     */
    public ImageIcon getEmojiIcon(String emojiCode) {
        // ✅ TRỢ LỰC: Nếu chưa trong cache, load ngay lập tức
        if (emojiCache.containsKey(emojiCode)) {
            return emojiCache.get(emojiCode);
        }
        
        String fileName = EMOJI_MAPPING.get(emojiCode);
        if (fileName == null) {
            return null;
        }
        
        // Load on-demand nếu preload miss
        try {
            InputStream is = getClass().getResourceAsStream("/resources/emoji/" + fileName);
            if (is == null) {
                is = getClass().getResourceAsStream("resources/emoji/" + fileName);
            }
            
            if (is != null) {
                byte[] imageBytes = is.readAllBytes();
                is.close();
                
                // Dùng BufferedImage (sync)
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes);
                java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(bis);
                bis.close();
                
                if (bufferedImage != null) {
                    java.awt.image.BufferedImage resizedImage = 
                        new java.awt.image.BufferedImage(EMOJI_SIZE, EMOJI_SIZE, 
                                                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g2d = resizedImage.createGraphics();
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                       java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(bufferedImage, 0, 0, EMOJI_SIZE, EMOJI_SIZE, null);
                    g2d.dispose();
                    
                    ImageIcon icon = new ImageIcon(resizedImage);
                    emojiCache.put(emojiCode, icon);
                    return icon;
                }
            }
        } catch (Exception e) {
            System.err.println("[EmojiManager] Error loading emoji " + emojiCode + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Lấy danh sách tất cả emoji codes
     */
    public String[] getAllEmojiCodes() {
        return EMOJI_MAPPING.keySet().toArray(new String[0]);
    }
    
    /**
     * Lấy emoji codes theo category
     */
    public String[] getEmojiCodesByCategory(String category) {
        // Có thể mở rộng sau để phân loại theo category
        return getAllEmojiCodes();
    }
    
    /**
     * Kiểm tra xem một code có phải là emoji code không
     */
    public boolean isEmojiCode(String code) {
        return EMOJI_MAPPING.containsKey(code);
    }
    
    /**
     * Thay thế tất cả emoji codes trong text bằng HTML img tag
     */
    /**
     * Thay thế tất cả emoji codes trong text bằng HTML img tag (Base64 inline)
     * ✅ FIXED: Dùng Base64 để embed ảnh trực tiếp - tránh lỗi path
     */
    /**
     * Thay thế emoji code (:smile:) bằng thẻ HTML <img>
     * FIX: Dùng URL Resource thay vì Base64 vì Swing không hỗ trợ Base64 trong HTML
     */
    public String replaceEmojiCodesWithHtml(String text) {
        if (text == null) return "";
        
        String result = text;
        
        // Duyệt qua map emoji
        for (Map.Entry<String, String> entry : EMOJI_MAPPING.entrySet()) {
            String code = entry.getKey();      // Ví dụ: :smile:
            String fileName = entry.getValue(); // Ví dụ: smile.png
            
            if (result.contains(code)) {
                // Lấy đường dẫn URL thực sự của file ảnh trong thư mục resources
                // Lưu ý: Đường dẫn bắt đầu bằng dấu / nghĩa là tìm từ thư mục gốc (src)
                java.net.URL imgUrl = getClass().getResource("/resources/emoji/" + fileName);
                
                // Fallback: Nếu null thì thử bỏ dấu / (tùy cấu trúc thư mục)
                if (imgUrl == null) {
                    imgUrl = getClass().getResource("resources/emoji/" + fileName);
                }

                if (imgUrl != null) {
                    // Swing hiển thị tốt các URL dạng file:/... hoặc jar:/...
                    String imgTag = "<img src='" + imgUrl.toString() + 
                                  "' width='" + EMOJI_SIZE + "' height='" + EMOJI_SIZE + 
                                  "' style='vertical-align:middle;' />";
                    
                    result = result.replace(code, imgTag);
                } else {
                    System.err.println("[EmojiManager] Không tìm thấy file: " + fileName);
                }
            }
        }
        return result;
    }
//    public String replaceEmojiCodesWithHtml(String text) {
//        if (text == null) return "";
//        
//        String result = text;
//        
//        // Duyệt qua tất cả các code emoji có trong map
//        for (Map.Entry<String, String> entry : EMOJI_MAPPING.entrySet()) {
//            String code = entry.getKey();
//            String fileName = entry.getValue();
//            
//            // Chỉ xử lý nếu text có chứa code này
//            if (!result.contains(code)) {
//                continue;
//            }
//            
//            try {
//                // ✅ LẤY INPUT STREAM TỪ RESOURCES
//                InputStream is = getClass().getResourceAsStream("/resources/emoji/" + fileName);
//                
//                if (is == null) {
//                    // Fallback: thử không có "/" đầu
//                    is = getClass().getResourceAsStream("resources/emoji/" + fileName);
//                }
//                
//                if (is != null) {
//                    // ✅ ĐỌC TOÀN BỘ BYTES VÀO MEMORY
//                    byte[] imageBytes = is.readAllBytes();
//                    is.close();
//                    
//                    // ✅ CONVERT THÀNH BASE64
//                    String base64String = java.util.Base64.getEncoder().encodeToString(imageBytes);
//                    
//                    // ✅ TẠO IMG TAG VỚI BASE64 INLINE
//                    String imgTag = "<img src='data:image/png;base64," + base64String + 
//                                  "' width='" + EMOJI_SIZE + "' height='" + EMOJI_SIZE + 
//                                  "' style='vertical-align:middle; margin: 0 2px;' />";
//                    
//                    result = result.replace(code, imgTag);
//                    
//                    System.out.println("[EmojiManager] ✅ Replaced: " + code + " (" + imageBytes.length + " bytes)");
//                } else {
//                    System.err.println("[EmojiManager] ❌ Không tìm thấy file ảnh cho: " + code);
//                }
//                
//            } catch (Exception e) {
//                System.err.println("[EmojiManager] ❌ Lỗi xử lý emoji " + code + ": " + e.getMessage());
//                e.printStackTrace();
//            }
//        }
//        
//        return result;
//    }
}

