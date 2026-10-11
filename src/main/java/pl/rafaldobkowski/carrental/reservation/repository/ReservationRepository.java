package pl.rafaldobkowski.carrental.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;

import java.time.LocalDate;
import java.util.Collection;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.car.id = :carId
              AND r.status IN :statuses
              AND r.startDate < :endDate
              AND r.endDate > :startDate
            """)
    boolean existsOverlappingReservation(
            @Param("carId") Long carId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses")
            Collection<ReservationStatus> statuses
    );
}