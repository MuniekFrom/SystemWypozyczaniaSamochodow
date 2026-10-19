package pl.rafaldobkowski.carrental.reservation.model;

import jakarta.persistence.*;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.user.model.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationStatusException;

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

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

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
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalPrice) {

        this.user = user;
        this.car = car;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
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
        if (status != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStatusException(
                    "Only confirmed reservations can be completed"
            );
        }

        status = ReservationStatus.COMPLETED;
    }

}