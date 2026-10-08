package pl.rafaldobkowski.carrental.user.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected User(){
    }

    public User(String firstName, String lastName, String email,
                String passwordHash, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        role = UserRole.CLIENT;
        status = UserStatus.ACTIVE;
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isBlocked() {
        return status == UserStatus.BLOCKED;
    }

    public void block() {
        status = UserStatus.BLOCKED;
    }

    public void activate() {
        status = UserStatus.ACTIVE;
    }
}
