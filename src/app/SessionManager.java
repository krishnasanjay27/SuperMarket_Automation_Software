package app;

public class SessionManager {

    private static SessionManager instance;

    private String userId;
    private String role;
    private String activeTransactionId;
    private int    activeCustomerId;
    private int    activeCustomerPoints;

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

    public String getActiveTransactionId()                                   { return activeTransactionId; }
    public void   setActiveTransactionId(String activeTransactionId)         { this.activeTransactionId = activeTransactionId; }

    public int  getActiveCustomerId()                                        { return activeCustomerId; }
    public void setActiveCustomerId(int activeCustomerId)                    { this.activeCustomerId = activeCustomerId; }

    public int  getActiveCustomerPoints()                                    { return activeCustomerPoints; }
    public void setActiveCustomerPoints(int activeCustomerPoints)            { this.activeCustomerPoints = activeCustomerPoints; }

    public void clear() {
        this.userId               = null;
        this.role                 = null;
        this.activeTransactionId  = null;
        this.activeCustomerId     = 0;
        this.activeCustomerPoints = 0;
    }
}
