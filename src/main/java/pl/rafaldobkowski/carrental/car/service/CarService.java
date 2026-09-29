package pl.rafaldobkowski.carrental.car.service;

import org.springframework.stereotype.Service;
import pl.rafaldobkowski.carrental.car.dto.CarResponse;
import pl.rafaldobkowski.carrental.car.dto.CreateCarRequest;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.car.repository.CarRepository;

@Service
public class CarService {

    private final CarRepository carRepository;

    public CarService(CarRepository carRepository){
        this.carRepository = carRepository;
    }

    public CarResponse createCar(CreateCarRequest request){
        Car car = new Car(request.brand(),
                request.model(),
                request.productionYear(),
                request.registrationNumber(),
                request.vin(),
                request.fuelType(),
                request.transmissionType(),
                request.bodyType(),
                request.numberOfSeats(),
                request.category(),
                request.color(),
                request.dailyPrice(),
                request.description());
        Car savedCar = carRepository.save(car);

        return new CarResponse(savedCar.getId(),
                savedCar.getBrand(),
                savedCar.getModel(),
                savedCar.getProductionYear(),
                savedCar.getRegistrationNumber(),
                savedCar.getVin(),
                savedCar.getFuelType(),
                savedCar.getTransmissionType(),
                savedCar.getBodyType(),
                savedCar.getNumberOfSeats(),
                savedCar.getCategory(),
                savedCar.getColor(),
                savedCar.getDailyPrice(),
                savedCar.getStatus(),
                savedCar.getDescription());
    }


}
