package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.audit.RejectionReason;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.Adopter;
import lv.bootcamp.shelter.model.AdoptionResult;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdoptionEligibilityServiceTest {

    private static final Long ADOPTER_ID = 1L;
    private static final Long ANIMAL_ID = 10L;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdoptionEligibilityService adoptionEligibilityService;

    @Test
    void evaluateAdoption_shouldRejectWhenAdopterIsNotFound() {
        when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.empty());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(false, RejectionReasons.ADOPTER_NOT_FOUND, 0));
        verifyNoInteractions(animalRepository, notificationClient, auditLogger);
    }

    @Test
    void evaluateAdoption_shouldRejectWhenAnimalIsNotFound() {
        when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.of(newAdopter()));
        when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.empty());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(false, RejectionReasons.ANIMAL_NOT_FOUND, 0));
        verifyNoInteractions(notificationClient, auditLogger);
    }

    @ParameterizedTest
    @EnumSource(value = AnimalStatus.class, names = {"ADOPTED", "RESERVED"})
    void evaluateAdoption_shouldRejectWhenAnimalIsNotAvailable(AnimalStatus status) {
        Animal animal = newAnimal();
        animal.setStatus(status);
        stubLookup(newAdopter(), animal);

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(false, RejectionReasons.ANIMAL_NOT_AVAILABLE, 0));
        verifyNoInteractions(notificationClient, auditLogger);
    }

    @Test
    void evaluateAdoption_shouldRejectUnderageAdopterAndRecordReason() {
        Adopter adopter = newAdopter();
        adopter.setAge(17);
        stubLookup(adopter, newAnimal());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(false, RejectionReasons.UNDERAGE, 0));
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.UNDERAGE);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldApproveAdopterWhoIsExactlyEighteen() {
        Adopter adopter = newAdopter();
        adopter.setAge(18);
        stubLookup(adopter, newAnimal());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(true, "Approved", 0));
        verify(auditLogger).logApproval(ADOPTER_ID, ANIMAL_ID, 0);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
    }

    @ParameterizedTest
    @CsvSource({"false, 4", "true, 6"})
    void evaluateAdoption_shouldRejectWhenPetLimitIsExceeded(boolean largeProperty, int petCount) {
        Adopter adopter = newAdopter();
        adopter.setLargeProperty(largeProperty);
        adopter.setCurrentPetCount(petCount);
        stubLookup(adopter, newAnimal());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(false, RejectionReasons.PET_LIMIT_REACHED, 0));
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.PET_LIMIT_REACHED);
        verifyNoInteractions(notificationClient);
    }

    @ParameterizedTest
    @CsvSource({"false, 3", "true, 5"})
    void evaluateAdoption_shouldRejectWhenPetCountEqualsLimit(boolean largeProperty, int petCount) {
        Adopter adopter = newAdopter();
        adopter.setLargeProperty(largeProperty);
        adopter.setCurrentPetCount(petCount);
        stubLookup(adopter, newAnimal());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(false, RejectionReasons.PET_LIMIT_REACHED, 0));
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.PET_LIMIT_REACHED);
        verifyNoInteractions(notificationClient);
    }

    @ParameterizedTest
    @CsvSource({"false, 2, 0", "true, 4, 7"})
    void evaluateAdoption_shouldApproveBelowPetLimit(boolean largeProperty, int petCount, int expectedScore) {
        Adopter adopter = newAdopter();
        adopter.setLargeProperty(largeProperty);
        adopter.setCurrentPetCount(petCount);
        stubLookup(adopter, newAnimal());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(true, "Approved", expectedScore));
        verify(auditLogger).logApproval(ADOPTER_ID, ANIMAL_ID, expectedScore);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
    }

    @ParameterizedTest
    @EnumSource(value = AnimalType.class, names = {"BIRD", "RABBIT"})
    void evaluateAdoption_shouldRejectExoticAnimalWithoutPermit(AnimalType type) {
        Animal animal = newAnimal();
        animal.setType(type);
        stubLookup(newAdopter(), animal);

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(false, RejectionReasons.EXOTIC_PERMIT_REQUIRED, 0));
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.NO_EXOTIC_PERMIT);
        verifyNoInteractions(notificationClient);
    }

    @ParameterizedTest
    @EnumSource(value = AnimalType.class, names = {"BIRD", "RABBIT"})
    void evaluateAdoption_shouldApproveExoticAnimalWithPermit(AnimalType type) {
        Adopter adopter = newAdopter();
        adopter.setExoticPermit(true);
        Animal animal = newAnimal();
        animal.setType(type);
        stubLookup(adopter, animal);

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(true, "Approved", 0));
        verify(auditLogger).logApproval(ADOPTER_ID, ANIMAL_ID, 0);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
    }

    @ParameterizedTest
    @EnumSource(value = AnimalType.class, names = {"DOG", "CAT", "OTHER"})
    void evaluateAdoption_shouldApproveNonExoticAnimalWithoutPermit(AnimalType type) {
        Animal animal = newAnimal();
        animal.setType(type);
        stubLookup(newAdopter(), animal);

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(true, "Approved", 0));
        verify(auditLogger).logApproval(ADOPTER_ID, ANIMAL_ID, 0);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
    }

    @Test
    void evaluateAdoption_shouldReturnPriorityScoreAndNotifyAdopter() {
        Adopter adopter = newAdopter();
        adopter.setPreviousAdoptions(4);
        adopter.setLargeProperty(true);
        adopter.setCurrentPetCount(2);
        Animal animal = newAnimal();
        animal.setAge(8);
        stubLookup(adopter, animal);

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result).isEqualTo(new AdoptionResult(true, "Approved", 46));
        verify(auditLogger).logApproval(ADOPTER_ID, ANIMAL_ID, 46);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
    }

    @ParameterizedTest
    @CsvSource({
            "0, false, 3, 0, 0",
            "1, false, 3, 0, 10",
            "3, false, 3, 0, 10",
            "4, false, 3, 0, 15",
            "0, true, 3, 0, 15",
            "0, false, 7, 0, 0",
            "0, false, 8, 0, 20",
            "0, false, , 0, 0",
            "1, false, 3, 2, 6",
            "0, false, 3, 1, 0",
            "4, true, 8, 2, 46"
    })
    void calculatePriorityScore_shouldApplyBonusesPenaltiesAndZeroFloor(
            int previousAdoptions, boolean largeProperty, Integer animalAge, int petCount, int expectedScore) {
        Adopter adopter = newAdopter();
        adopter.setPreviousAdoptions(previousAdoptions);
        adopter.setLargeProperty(largeProperty);
        adopter.setCurrentPetCount(petCount);
        Animal animal = newAnimal();
        animal.setAge(animalAge);

        int score = adoptionEligibilityService.calculatePriorityScore(adopter, animal);

        assertThat(score).isEqualTo(expectedScore);
    }

    private void stubLookup(Adopter adopter, Animal animal) {
        when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(animal));
    }

    private Adopter newAdopter() {
        Adopter adopter = new Adopter();
        adopter.setId(ADOPTER_ID);
        adopter.setName("Anna");
        adopter.setEmail("anna@example.com");
        adopter.setAge(30);
        adopter.setCurrentPetCount(0);
        adopter.setPreviousAdoptions(0);
        adopter.setLargeProperty(false);
        adopter.setExoticPermit(false);
        return adopter;
    }

    private Animal newAnimal() {
        Animal animal = new Animal();
        animal.setId(ANIMAL_ID);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setAge(3);
        animal.setStatus(AnimalStatus.AVAILABLE);
        return animal;
    }
}
