package Controll;

public enum MessageType {
    // ============== CLIENT GỬI LÊN SERVER ==============
    LOGIN_REQUEST,              // Yêu cầu đăng nhập
    REGISTER_REQUEST,           // Yêu cầu đăng ký
    LOGOUT_REQUEST,             // Yêu cầu đăng xuất
    
    FRIEND_LIST_REQUEST,        // Yêu cầu danh sách bạn bè
    FRIEND_REQUEST_SEND,        // Gửi lời mời kết bạn
    FRIEND_REQUEST_ACCEPT,      // Chấp nhận lời mời kết bạn
    REQUEST_PENDING_FRIENDS,    // Yêu cầu danh sách lời mời đang chờ
    USER_SEARCH_REQUEST,        // Tìm kiếm người dùng (theo số điện thoại)
    
    MESSAGE_SEND,               // Gửi tin nhắn
    HISTORY_REQUEST,            // Yêu cầu lịch sử chat với 1 người

    // ============== SERVER GỬI VỀ CLIENT ==============
    LOGIN_SUCCESS,              // Đăng nhập thành công (kèm User object)
    LOGIN_FAILURE,              // Đăng nhập thất bại (kèm message lỗi)
    REGISTER_SUCCESS,           // Đăng ký thành công
    REGISTER_FAILURE,           // Đăng ký thất bại
    
    FRIEND_LIST_RESPONSE,       // Trả về danh sách bạn bè (List<User>)
    FRIEND_REQUEST_SUCCESS,     // Gửi lời mời thành công
    FRIEND_REQUEST_FAILURE,     // Gửi lời mời thất bại
    FRIEND_REQUEST_RECEIVED,    // Có người gửi lời mời cho bạn (kèm User object)
    FRIEND_REQUEST_ACCEPTED,    // Có người chấp nhận lời mời của bạn (kèm User object)
    PENDING_FRIEND_REQUESTS_RESPONSE, // Danh sách lời mời đang chờ (List<User>)
    USER_SEARCH_RESPONSE,       // Kết quả tìm kiếm người dùng
    
    MESSAGE_RECEIVE,            // Nhận tin nhắn mới từ người khác
    HISTORY_RESPONSE,           // Trả về lịch sử chat (List<Message>)
    
    USER_ONLINE_NOTICE,         // Thông báo bạn bè online
    USER_OFFLINE_NOTICE,         // Thông báo bạn bè offline
    GROUP_CREATE_SUCCESS,
    GROUP_CREATE_FAILURE,
    GROUP_CREATE_REQUEST,
    GROUP_CREATED_NOTICE,
    CONVERSATION_LIST_REQUEST,
    CONVERSATION_LIST_RESPONSE,
    
    CALL_REQUEST,   // A yêu cầu gọi B
    CALL_ACCEPT,    // B đồng ý
    CALL_DENY,      // B từ chối
    CALL_END        // Kết thúc gọi
, VIDEO_CALL_REQUEST,

}