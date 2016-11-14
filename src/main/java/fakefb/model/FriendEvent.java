package fakefb.model;

public class FriendEvent {

    Friend from;
    Friend to;
    boolean areFriends;
    long timestamp;

    public Friend getFrom() {
        return from;
    }

    public void setFrom(Friend from) {
        this.from = from;
    }

    public Friend getTo() {
        return to;
    }

    public void setTo(Friend to) {
        this.to = to;
    }

    public boolean isAreFriends() {
        return areFriends;
    }

    public void setAreFriends(boolean areFriends) {
        this.areFriends = areFriends;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
