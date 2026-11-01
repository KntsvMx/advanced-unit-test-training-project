package lv.bootcamp.shelter;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.dto.AdoptionRequest;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
public class AdoptionIntegrationTest {

    @Autowired
    private AnimalService animalService;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private NotificationClient notificationClient;

    @Test
    void createAndAdopt_shouldSaveAdoptedAnimalAndNotify() {
        AnimalCreateRequest animalCreateRequest = new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador", 3, "Friendly", null);

        AnimalResponse animalResponse = animalService.create(animalCreateRequest);

        assertThat(animalResponse).isNotNull();
        assertThat(animalResponse.id()).isNotNull();
        assertThat(animalResponse.name()).isEqualTo("Rex");
        assertThat(animalResponse.status()).isEqualTo(AnimalStatus.AVAILABLE);

        AdoptionRequest adoptionRequest = new AdoptionRequest(animalResponse.id(), "Anna", "anna@example.com");
        AnimalResponse animalResponse1 = animalService.adopt(adoptionRequest);

        assertThat(animalResponse1).isNotNull();
        assertThat(animalResponse1.id()).isNotNull();
        assertThat(animalResponse1.id()).isEqualTo(animalResponse.id());
        assertThat(animalResponse1.status()).isEqualTo(AnimalStatus.ADOPTED);

        entityManager.flush();
        entityManager.clear();

        Animal savedAnimal = animalRepository.findById(animalResponse.id()).orElseThrow();
        assertThat(savedAnimal.getName()).isEqualTo("Rex");
        assertThat(savedAnimal.getStatus()).isEqualTo(AnimalStatus.ADOPTED);

        verify(notificationClient).sendAdoptionNotification(animalResponse.id(), "Rex", "anna@example.com");
    }
}
