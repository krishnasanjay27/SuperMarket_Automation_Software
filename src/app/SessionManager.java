package app;

public class SessionManager {

    private static SessionManager instance;

    private String userId;
    private String role;
    private String activeTransactionId;

    private SessionManager() { }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public String getUserId()                          { return userId; }
    public void   setUserId(String userId)             { this.userId = userId; }

    public String getRole()                            { return role; }
    public void   setRole(String role)                 { this.role = role; }

    public String getActiveTransactionId()                                    { return activeTransactionId; }
    public void   setActiveTransactionId(String activeTransactionId)          { this.activeTransactionId = activeTransactionId; }

    public void clear() {
        this.userId              = null;
        this.role                = null;
        this.activeTransactionId = null;
    }
}
