package pl.rafaldobkowski.carrental.reservation.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.car.id = :carId
              AND r.status IN :statuses
              AND r.startAt < :blockingWindowEnd
              AND r.endAt > :blockingWindowStart
            """)
    boolean existsOverlappingReservation(
            @Param("carId") Long carId,
            @Param("blockingWindowStart") LocalDateTime blockingWindowStart,
            @Param("blockingWindowEnd") LocalDateTime blockingWindowEnd,
            @Param("statuses") Collection<ReservationStatus> statuses
    );

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.car.id = :carId
              AND r.status IN :statuses
              AND r.startAt < :rangeEnd
              AND r.endAt > :rangeStart
            ORDER BY r.startAt
            """)
    List<Reservation> findBlockingReservationsForCar(
            @Param("carId") Long carId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("statuses") Collection<ReservationStatus> statuses
    );

    @EntityGraph(attributePaths = {"user", "car"})
    List<Reservation> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Reservation> findByIdAndUserId(
            Long reservationId,
            Long userId
    );

    @EntityGraph(attributePaths = {"user", "car"})
    List<Reservation> findAllByOrderByCreatedAtDesc();
}
