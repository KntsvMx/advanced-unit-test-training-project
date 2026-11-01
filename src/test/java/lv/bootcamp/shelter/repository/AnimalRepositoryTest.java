package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ticket: ANIMAL-3 (see README).
 * findByStatus_shouldReturnOnlyMatchingAnimals is already implemented —
 * mirror this pattern (and AdopterRepositoryTest's constraint test) to add
 * the second method yourself. You'll also need to create
 * AdoptionRecordRepositoryTest from scratch (see README).
 */
@DataJpaTest
class AnimalRepositoryTest {

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByStatus_shouldReturnOnlyMatchingAnimals() {
        entityManager.persist(newAnimal("Rex", AnimalType.DOG, AnimalStatus.AVAILABLE));
        entityManager.persist(newAnimal("Mia", AnimalType.CAT, AnimalStatus.ADOPTED));
        entityManager.flush();

        var available = animalRepository.findByStatus(AnimalStatus.AVAILABLE);

        assertThat(available).hasSize(1);
        assertThat(available.getFirst().getName()).isEqualTo("Rex");
    }

    private Animal newAnimal(String name, AnimalType type, AnimalStatus status) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setStatus(status);
        return animal;
    }

    @Test
    void create_shouldBeRejectedWithSimilarMicrochipIdAndNotSaved() {
        entityManager.persist(newAnimalWithMicrochipId("Rex", AnimalType.DOG, AnimalStatus.AVAILABLE, "123456"));
        entityManager.flush();

        Animal animalWithIdenticalMicrochipID = newAnimalWithMicrochipId("Buddy", AnimalType.DOG, AnimalStatus.AVAILABLE, "123456");

        assertThatThrownBy(() -> {
            animalRepository.save(animalWithIdenticalMicrochipID);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Animal newAnimalWithMicrochipId(String name, AnimalType type, AnimalStatus status, String microchipId) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setStatus(status);
        animal.setMicrochipId(microchipId);
        return animal;
    }

}
