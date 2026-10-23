package pl.rafaldobkowski.carrental.car.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.car.model.CarStatus;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CarRepository extends JpaRepository<Car,Long> {

    boolean existsByVin(String vin);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByVinAndIdNot(String vin, Long id);

    boolean existsByRegistrationNumberAndIdNot(
            String registrationNumber,
            Long id
    );

    @Query("""
        SELECT c
        FROM Car c
        WHERE c.status = :carStatus
          AND NOT EXISTS (
              SELECT r.id
              FROM Reservation r
              WHERE r.car = c
                AND r.status IN :reservationStatuses
                AND r.startDate < :endDate
                AND r.endDate > :startDate
          )
        ORDER BY c.id
        """)
    List<Car> findAvailableCars(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("carStatus") CarStatus carStatus,
            @Param("reservationStatuses")
            Collection<ReservationStatus> reservationStatuses
    );

}
