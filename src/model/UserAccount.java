package model;

import java.time.LocalDateTime;

public class UserAccount {

    private String        userId;
    private String        password;
    private String        role;
    private LocalDateTime createdAt;
    private String        status;

    public UserAccount() { }

    public UserAccount(String userId, String password, String role,
                       LocalDateTime createdAt, String status) {
        this.userId    = userId;
        this.password  = password;
        this.role      = role;
        this.createdAt = createdAt;
        this.status    = status;
    }

    public String getUserId()                       { return userId; }
    public void   setUserId(String userId)          { this.userId = userId; }

    public String getPassword()                     { return password; }
    public void   setPassword(String password)      { this.password = password; }

    public String getRole()                         { return role; }
    public void   setRole(String role)              { this.role = role; }

    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus()                       { return status; }
    public void   setStatus(String status)          { this.status = status; }

    @Override
    public String toString() {
        return "UserAccount{" +
               "userId='"   + userId   + '\'' +
               ", role='"   + role     + '\'' +
               ", status='" + status   + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}
