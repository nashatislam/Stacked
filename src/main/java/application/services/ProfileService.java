package application.services;

import application.Realm;

public interface ProfileService {
    String getUsername();
    void setUsername(String name);
    boolean hasProfile();
    Realm getLastRealm();
    void setLastRealm(Realm realm);
}