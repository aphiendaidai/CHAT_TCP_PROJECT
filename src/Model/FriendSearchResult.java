package Model;

import java.io.Serializable;

public class FriendSearchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        FOUND,
        NOT_FOUND,
        SELF,
        ALREADY_CONNECTED
    }

    private Status status;
    private User user;
    private String query;

    public FriendSearchResult() {}

    public FriendSearchResult(Status status, User user, String query) {
        this.status = status;
        this.user = user;
        this.query = query;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}

