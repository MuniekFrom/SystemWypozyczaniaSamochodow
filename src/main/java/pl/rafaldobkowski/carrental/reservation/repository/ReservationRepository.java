package pl.rafaldobkowski.carrental.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

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

    @EntityGraph(attributePaths = {"user", "car"})
    List<Reservation> findAllByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    Optional<Reservation> findByIdAndUserId(
            Long reservationId,
            Long userId
    );
}