package pl.rafaldobkowski.carrental.reservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationStatusException;
import pl.rafaldobkowski.carrental.user.model.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Reservation() {
    }

    public Reservation(
            User user,
            Car car,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal totalPrice) {

        this.user = user;
        this.car = car;
        this.startAt = startAt;
        this.endAt = endAt;
        this.totalPrice = totalPrice;
        this.status = ReservationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Car getCar() {
        return car;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING
                && status != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStatusException(
                    "Reservation cannot be cancelled in its current status"
            );
        }

        status = ReservationStatus.CANCELLED;
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new InvalidReservationStatusException(
                    "Only pending reservations can be confirmed"
            );
        }

        status = ReservationStatus.CONFIRMED;
    }

    public void complete() {
        if (status != ReservationStatus.IN_PROGRESS) {
            throw new InvalidReservationStatusException(
                    "Only in-progress reservations can be completed"
            );
        }

        status = ReservationStatus.COMPLETED;
    }

    public void startRental() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStatusException(
                    "Only confirmed reservations can be started"
            );
        }

        status = ReservationStatus.IN_PROGRESS;
    }
}
