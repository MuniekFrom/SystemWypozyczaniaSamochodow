package pl.rafaldobkowski.carrental.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.rafaldobkowski.carrental.user.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

}
