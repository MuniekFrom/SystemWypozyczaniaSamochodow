package pl.rafaldobkowski.carrental.user.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import pl.rafaldobkowski.carrental.user.model.User;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");


    @Autowired
    private UserRepository userRepository;

    private User createTestUser(){
        return new User("Anna", "Nowak", "anna.nowak@example.com", "test-password-hash", "+48123456789"
        );
    }

    @Test
    void shouldSaveUserInDatabase() {
        User user = createTestUser();

        User savedUser = userRepository.saveAndFlush(user);


        assertNotNull(savedUser.getId());

        assertTrue(userRepository.existsByEmail(savedUser.getEmail()));


    }

    @Test
    void shouldRejectDuplicateEmail() {

        User user1 = new User("Robert", "Makłowicz", "rm9@gmail.com", "test12test", "+48987654321");
        User user2 = new User("Robert", "Marynarz", "rm9@gmail.com", "test13test", " +48789321321");

        userRepository.saveAndFlush(user1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(user2)
        );



    }

}
