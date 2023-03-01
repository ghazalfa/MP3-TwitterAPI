package pheme;

import java.util.UUID;

public class PhemeServiceUser {
    private UUID userID;
    private String userName;
    private String hashPassword;

    public PhemeServiceUser(UUID userID, String userName, String hashPassword) {
        this.userID = userID;
        this.userName = userName;
        this. hashPassword = hashPassword;
    }
}
