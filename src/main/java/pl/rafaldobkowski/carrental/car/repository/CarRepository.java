package pl.rafaldobkowski.carrental.car.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.rafaldobkowski.carrental.car.model.Car;

public interface CarRepository extends JpaRepository<Car,Long> {

    boolean existsByVin(String vin);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByVinAndIdNot(String vin, Long id);

    boolean existsByRegistrationNumberAndIdNot(
            String registrationNumber,
            Long id
    );

}
