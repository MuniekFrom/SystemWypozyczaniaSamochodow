package pl.rafaldobkowski.carrental.car.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarTest {

    @Test
    void shouldUpdateTechnicalData() {
        Car car = new Car(
                "Toyota",
                "4Runner TRD Off-Road",
                2018,
                "BI 4RUN1",
                "JTEBU5JR0J5000001",
                CarFuelType.GASOLINE,
                CarTransmissionType.AUTOMATIC,
                CarBodyType.SUV,
                5,
                CarCategory.PREMIUM,
                "Silver",
                new BigDecimal("449.99"),
                "Terenowy samochód z napędem 4x4"
        );

        car.updateTechnicalData(
                3956,
                270,
                CarDriveType.FOUR_WHEEL_DRIVE,
                new BigDecimal("7.50")
        );

        assertEquals(3956, car.getEngineCapacityCm3());
        assertEquals(270, car.getPowerHp());
        assertEquals(
                CarDriveType.FOUR_WHEEL_DRIVE,
                car.getDriveType()
        );
        assertEquals(
                new BigDecimal("7.50"),
                car.getAccelerationTo100Seconds()
        );
    }
}