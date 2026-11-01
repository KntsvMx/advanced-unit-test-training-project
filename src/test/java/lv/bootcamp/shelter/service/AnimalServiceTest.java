package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.dto.AdoptionRequest;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ticket: ANIMAL-1 (see README).
 * Three tests are already implemented as a worked example (also see
 * AdopterServiceTest for the same pattern) — add the remaining tests yourself.
 */
@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private AnimalService animalService;

    @Test
    void create_shouldSaveAnimalWithAvailableStatus() {
        Animal saved = new Animal();
        saved.setId(1L);
        saved.setName("Rex");
        saved.setType(AnimalType.DOG);
        saved.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.save(any(Animal.class))).thenReturn(saved);

        AnimalResponse response = animalService.create(
                new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador", 3, "Friendly", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Rex");
        assertThat(response.status()).isEqualTo(AnimalStatus.AVAILABLE);
    }

    @Test
    void findById_shouldThrowWhenAnimalNotFound() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.findById(99L))
                .isInstanceOf(AnimalNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void adopt_shouldThrowWhenAnimalAlreadyAdopted() {
        Animal adopted = new Animal();
        adopted.setId(1L);
        adopted.setStatus(AnimalStatus.ADOPTED);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(adopted));

        assertThatThrownBy(() -> animalService.adopt(
                new AdoptionRequest(1L, "Anna", "anna@example.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adopt_shouldChangeAvailableStatusAndTriggerAdoptionNotification() {
        Animal animal = new Animal();
        animal.setId(1L);
        animal.setName("Rex");
        animal.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(animalRepository.save(animal)).thenReturn(animal);

        animalService.adopt(new AdoptionRequest(1L, "Anna", "anna@example.com"));

        assertThat(animal.getStatus()).isEqualTo(AnimalStatus.ADOPTED);
        verify(notificationClient).sendAdoptionNotification(1L, "Rex", "anna@example.com");
    }

    @Test
    void reserveMultiple_shouldNotifyExternalSystemWithIds() {
        List<Long> animalsIds = List.of(1L, 2L, 3L);
        List<Animal> animalList = animalsIds.stream().map(id -> {
            Animal animal = new Animal();
            animal.setId(id);
            animal.setStatus(AnimalStatus.AVAILABLE);
            animal.setName("Rex" + id);
            return animal;
        }).toList();


        when(animalRepository.findAllById(animalsIds)).thenReturn(animalList);
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        animalService.reserveMultiple(animalsIds);
        verify(notificationClient).sendBulkStatusNotification(animalsIds, "RESERVED");
    }

    @Test
    void create_shouldBeRejectedWithSimilarMicrochipId() {
        Animal animal = new Animal();
        animal.setMicrochipId("id12345");

        when(animalRepository.existsByMicrochipId(animal.getMicrochipId())).thenReturn(true);

        assertThatThrownBy(() -> animalService.create(new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador", 3, "Friendly", "id12345")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage( "Animal with microchip %s is already registered".formatted(animal.getMicrochipId()));

        verify(animalRepository, times(0)).save(any(Animal.class));
    }
}
