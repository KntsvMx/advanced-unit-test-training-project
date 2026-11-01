package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.AdoptionRecord;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AdoptionRecordRepositoryTest {

    @Autowired
    private AdoptionRecordRepository adoptionRecordRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findRecentAdoptionsByAnimalType_shouldReturnAdoptedAnimalConsideringDateAndType() {
        LocalDateTime since = LocalDateTime.of(2026, 11, 1, 0, 0);
        Animal animal1 = entityManager.persist(newAnimal("Buddy", AnimalType.DOG, "Golden Retriever", 3, AnimalStatus.ADOPTED, "MC123456"));
        Animal animal2 = entityManager.persist(newAnimal("Buddy1", AnimalType.CAT, "Golden", 2, AnimalStatus.ADOPTED, "MC1234567"));
        Animal animal3 = entityManager.persist(newAnimal("Buddy2", AnimalType.DOG, "Retriever", 1, AnimalStatus.ADOPTED, "MC12345678"));

        entityManager.persist(new AdoptionRecord(null, animal1, "anna@example.com", since.plusDays(1)));
        entityManager.persist(new AdoptionRecord(null, animal2, "bob@example.com", since.plusDays(1)));
        entityManager.persist(new AdoptionRecord(null, animal3, "charlie@example.com", since.minusDays(2)));
        entityManager.flush();

        List<AdoptionRecord> adoptionRecordsList = adoptionRecordRepository.findRecentAdoptionsByAnimalType(AnimalType.DOG, since);

        assertThat(adoptionRecordsList).hasSize(1);
        assertThat(adoptionRecordsList.getFirst().getAnimal()).isEqualTo(animal1);
    }

    private Animal newAnimal(String name, AnimalType type, String breed, Integer age, AnimalStatus status, String microchipId) {
        {
            Animal animal = new Animal();
            animal.setName(name);
            animal.setType(type);
            animal.setBreed(breed);
            animal.setAge(age);
            animal.setStatus(status);
            animal.setMicrochipId(microchipId);
            return animal;
        }


    }
}
