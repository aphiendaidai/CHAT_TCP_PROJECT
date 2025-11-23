package DataAccess;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class CloudinaryUtils {
    private static Cloudinary cloudinary;
    
    static {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dqqbvzmrr",
            "api_key", "789644833813378",
            "api_secret", "15CW3h71RPsMQWIJ60tIVkRhbO0"));
    }
    
    /**
     * Upload ảnh lên Cloudinary từ File
     * @param file file ảnh
     * @return URL ảnh sau khi upload (HTTPS)
     * @throws Exception nếu upload lỗi
     */
    @SuppressWarnings("unchecked")
    public static String uploadImage(File file) throws Exception {
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
        return (String) uploadResult.get("secure_url");
    }
    
    /**
     * Upload ảnh lên Cloudinary từ byte array (dùng khi nhận Base64 từ client)
     * @param imageBytes mảng byte của ảnh
     * @param fileName tên file (để Cloudinary biết format)
     * @return URL ảnh sau khi upload (HTTPS)
     * @throws Exception nếu upload lỗi
     */
    public static String uploadImageFromBytes(byte[] imageBytes, String fileName) throws Exception {
        Path tempFile = Files.createTempFile("upload_", "_" + fileName);
        try {
            Files.write(tempFile, imageBytes);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile.toFile(), ObjectUtils.asMap(
                "folder", "chat_images",
                "resource_type", "image"
            ));
            
            return (String) uploadResult.get("secure_url");
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                System.err.println("Warning: Could not delete temp file: " + tempFile);
            }
        }
    }
    
    /**
     * Upload ảnh với options tùy chỉnh
     * @param imageBytes mảng byte của ảnh
     * @param fileName tên file
     * @param options Map chứa các options (folder, transformation, etc.)
     * @return URL ảnh sau khi upload
     * @throws Exception nếu upload lỗi
     */
    public static String uploadImageFromBytes(byte[] imageBytes, String fileName, Map<String, Object> options) throws Exception {
        Path tempFile = Files.createTempFile("upload_", "_" + fileName);
        try {
            Files.write(tempFile, imageBytes);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> defaultOptions = (Map<String, Object>) ObjectUtils.asMap(
                "folder", "chat_images",
                "resource_type", "image"
            );
            Map<String, Object> uploadOptions = new java.util.HashMap<>(defaultOptions);
            if (options != null) {
                uploadOptions.putAll(options);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile.toFile(), uploadOptions);
            return (String) uploadResult.get("secure_url");
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                System.err.println("Warning: Could not delete temp file: " + tempFile);
            }
        }
    }
    
    // ==================== FILE UPLOAD METHODS ====================
    
    /**
     * Upload file (PDF, DOC, TXT, v.v.) lên Cloudinary từ File
     * @param file file cần upload
     * @return URL file sau khi upload (HTTPS)
     * @throws Exception nếu upload lỗi
     */
    @SuppressWarnings("unchecked")
    public static String uploadFile(File file) throws Exception {
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
            "folder", "chat_files",
            "resource_type", "raw" // "raw" cho các file không phải image/video
        ));
        return (String) uploadResult.get("secure_url");
    }
    
    /**
     * Upload file từ byte array
     * @param fileBytes mảng byte của file
     * @param fileName tên file (bắt buộc để Cloudinary xác định extension)
     * @return URL file sau khi upload (HTTPS)
     * @throws Exception nếu upload lỗi
     */
    public static String uploadFileFromBytes(byte[] fileBytes, String fileName) throws Exception {
        Path tempFile = Files.createTempFile("upload_", "_" + fileName);
        try {
            Files.write(tempFile, fileBytes);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile.toFile(), ObjectUtils.asMap(
                "folder", "chat_files",
                "resource_type", "raw",
                "public_id", getFileNameWithoutExtension(fileName) // Giữ tên file gốc
            ));
            
            return (String) uploadResult.get("secure_url");
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                System.err.println("Warning: Could not delete temp file: " + tempFile);
            }
        }
    }
    
    /**
     * Upload file với options tùy chỉnh
     * @param fileBytes mảng byte của file
     * @param fileName tên file
     * @param options Map chứa các options (folder, access_mode, etc.)
     * @return URL file sau khi upload
     * @throws Exception nếu upload lỗi
     */
    public static String uploadFileFromBytes(byte[] fileBytes, String fileName, Map<String, Object> options) throws Exception {
        Path tempFile = Files.createTempFile("upload_", "_" + fileName);
        try {
            Files.write(tempFile, fileBytes);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> defaultOptions = (Map<String, Object>) ObjectUtils.asMap(
                "folder", "chat_files",
                "resource_type", "raw"
            );
            Map<String, Object> uploadOptions = new java.util.HashMap<>(defaultOptions);
            if (options != null) {
                uploadOptions.putAll(options);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile.toFile(), uploadOptions);
            return (String) uploadResult.get("secure_url");
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                System.err.println("Warning: Could not delete temp file: " + tempFile);
            }
        }
    }
    
    /**
     * Upload file với thông tin chi tiết trả về
     * @param fileBytes mảng byte của file
     * @param fileName tên file
     * @return Map chứa thông tin file (url, public_id, format, bytes, v.v.)
     * @throws Exception nếu upload lỗi
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> uploadFileWithDetails(byte[] fileBytes, String fileName) throws Exception {
        Path tempFile = Files.createTempFile("upload_", "_" + fileName);
        try {
            Files.write(tempFile, fileBytes);
            
            Map<String, Object> uploadResult = cloudinary.uploader().upload(tempFile.toFile(), ObjectUtils.asMap(
                "folder", "chat_files",
                "resource_type", "raw",
                "public_id", getFileNameWithoutExtension(fileName)
            ));
            
            return uploadResult;
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                System.err.println("Warning: Could not delete temp file: " + tempFile);
            }
        }
    }
    
    /**
     * Xóa file/image từ Cloudinary
     * @param publicId public_id của file trên Cloudinary
     * @param resourceType "image" hoặc "raw"
     * @return Map kết quả xóa
     * @throws Exception nếu xóa lỗi
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deleteResource(String publicId, String resourceType) throws Exception {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
            "resource_type", resourceType
        ));
    }
    
    /**
     * Lấy tên file không có extension
     * @param fileName tên file đầy đủ
     * @return tên file không có extension
     */
    private static String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
    
    /**
     * Kiểm tra file có phải là image không
     * @param fileName tên file
     * @return true nếu là image
     */
    public static boolean isImageFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".jpg") || 
               lowerCaseName.endsWith(".jpeg") || 
               lowerCaseName.endsWith(".png") || 
               lowerCaseName.endsWith(".gif") || 
               lowerCaseName.endsWith(".bmp") || 
               lowerCaseName.endsWith(".webp");
    }
}