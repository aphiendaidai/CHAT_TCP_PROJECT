package Model;

public class Friendship {
    private int requesterId;
    private int addresseeId;
    private FriendshipStatus status; // Sử dụng Enum ở trên

    // Constructor, Getters, Setters
    
    public Friendship() {}

    public Friendship(int requesterId, int addresseeId, FriendshipStatus status) {
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.status = status;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(int requesterId) {
        this.requesterId = requesterId;
    }

    public int getAddresseeId() {
        return addresseeId;
    }

    public void setAddresseeId(int addresseeId) {
        this.addresseeId = addresseeId;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }
}