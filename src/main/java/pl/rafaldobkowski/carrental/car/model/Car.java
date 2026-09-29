package pl.rafaldobkowski.carrental.car.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false)
    private Integer productionYear;

    @Column(nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CarFuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CarTransmissionType transmissionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CarBodyType bodyType;

    @Column(nullable = false)
    private Integer numberOfSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CarCategory category;

    @Column(nullable = false, length = 30)
    private String color;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CarStatus status;

    @Column(length = 1000)
    private String description;

    protected Car(){
    }

    public Long getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public Integer getProductionYear() {
        return productionYear;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getVin() {
        return vin;
    }

    public CarFuelType getFuelType() {
        return fuelType;
    }

    public CarTransmissionType getTransmissionType() {
        return transmissionType;
    }

    public CarBodyType getBodyType() {
        return bodyType;
    }

    public Integer getNumberOfSeats() {
        return numberOfSeats;
    }

    public CarCategory getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public CarStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public Car(String brand, String model, Integer productionYear, String registrationNumber, String vin, CarFuelType fuelType, CarTransmissionType transmissionType, CarBodyType bodyType, Integer numberOfSeats, CarCategory category, String color, BigDecimal dailyPrice, String description) {
        this.brand = brand;
        this.model = model;
        this.productionYear = productionYear;
        this.registrationNumber = registrationNumber;
        this.vin = vin;
        this.fuelType = fuelType;
        this.transmissionType = transmissionType;
        this.bodyType = bodyType;
        this.numberOfSeats = numberOfSeats;
        this.category = category;
        this.color = color;
        this.dailyPrice = dailyPrice;
        this.status = CarStatus.ACTIVE;
        this.description = description;
    }

    public void updateDetails(
            String brand,
            String model,
            Integer productionYear,
            String registrationNumber,
            String vin,
            CarFuelType fuelType,
            CarTransmissionType transmissionType,
            CarBodyType bodyType,
            Integer numberOfSeats,
            CarCategory category,
            String color,
            BigDecimal dailyPrice,
            String description
    ) {
        this.brand = brand;
        this.model = model;
        this.productionYear = productionYear;
        this.registrationNumber = registrationNumber;
        this.vin = vin;
        this.fuelType = fuelType;
        this.transmissionType = transmissionType;
        this.bodyType = bodyType;
        this.numberOfSeats = numberOfSeats;
        this.category = category;
        this.color = color;
        this.dailyPrice = dailyPrice;
        this.description = description;
    }

    // Zmienia status samochodu
    public void changeStatus(CarStatus newStatus) {
        this.status = newStatus;
    }
}
