package Model;

import java.io.Serializable;

public enum FriendshipStatus implements Serializable  {
    PENDING,  // Đang chờ
    ACCEPTED, // Đã chấp nhận
    BLOCKED   // Đã chặn
}