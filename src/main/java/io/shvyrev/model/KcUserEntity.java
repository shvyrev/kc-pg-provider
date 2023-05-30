package io.shvyrev.model;

import javax.persistence.*;
import java.util.UUID;

@NamedQueries({
        @NamedQuery(name="getUserByUsername", query="select u from KcUserEntity u where u.username=:username"),
        @NamedQuery(name="getUserCount", query="select count(u) from KcUserEntity u"),
        @NamedQuery(name="getAllUsers", query="select u from KcUserEntity u"),
        @NamedQuery(name="searchForUser", query="select u from KcUserEntity u where " + "( u.username like :search )")
})
@Entity
@Table(name = "users")
public class KcUserEntity {

    @Id
    private UUID id;
    private String username;
    private String password;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
