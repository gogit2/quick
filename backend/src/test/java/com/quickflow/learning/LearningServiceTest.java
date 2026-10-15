package com.quickflow.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quickflow.common.ConflictException;
import com.quickflow.common.NotFoundException;
import com.quickflow.common.PlanItemSourceType;
import com.quickflow.common.SourceDeletedEvent;
import com.quickflow.support.MutableClock;
import com.quickflow.support.MutableClockTestConfig;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** Business rules of LearningService on a real (file) H2 database with a controllable clock. */
@SpringBootTest
@Import(MutableClockTestConfig.class)
@RecordApplicationEvents
@TestPropertySource(properties = "qf.db.path=./target/test-data/learning-service-${random.uuid}")
class LearningServiceTest {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");

    @Autowired
    private LearningService service;

    @Autowired
    private LearningCardRepository cards;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void resetState() {
        cards.deleteAll();
        clock.set(START);
    }

    @Test
    void newCardWithoutStatusIsNotStartedAndHasNoChildren() {
        LearningCard card = service.create(new LearningCardCreate("Kotlin", null, null));

        assertThat(card.status()).isEqualTo(LearningStatus.NOT_STARTED);
        assertThat(card.createdAt()).isEqualTo(START);
        assertThat(card.milestones()).isEmpty();
        assertThat(card.notes()).isEmpty();
    }

    @Test
    void statusFollowsMilestonesWhenAddedCompletedAndUncompleted() {
        LearningCard card = service.create(new LearningCardCreate("Kotlin", null, null));
        LearningMilestone first = service.addMilestone(card.id(),
                new LearningMilestoneCreate("Basics", LocalDate.of(2026, 10, 30)));
        LearningMilestone second = service.addMilestone(card.id(), new LearningMilestoneCreate("Coroutines", null));
        assertThat(first.id()).isNotNull();
        assertThat(status(card.id())).isEqualTo(LearningStatus.NOT_STARTED);

        service.updateMilestone(card.id(), first.id(), new LearningMilestoneUpdate("Basics", true, null));
        assertThat(status(card.id())).isEqualTo(LearningStatus.IN_PROGRESS);

        service.updateMilestone(card.id(), second.id(), new LearningMilestoneUpdate("Coroutines", true, null));
        assertThat(status(card.id())).isEqualTo(LearningStatus.COMPLETED);

        service.updateMilestone(card.id(), first.id(), new LearningMilestoneUpdate("Basics", false, null));
        assertThat(status(card.id())).isEqualTo(LearningStatus.IN_PROGRESS);
    }

    @Test
    void completedAtIsSetWhenDoneTurnsTrueAndClearedWhenItTurnsFalse() {
        LearningCard card = service.create(new LearningCardCreate("Kotlin", null, null));
        LearningMilestone milestone = service.addMilestone(card.id(), new LearningMilestoneCreate("Basics", null));

        clock.set(START.plusSeconds(60));
        LearningMilestone done = service.updateMilestone(card.id(), milestone.id(),
                new LearningMilestoneUpdate("Basics", true, null));
        assertThat(done.completedAt()).isEqualTo(START.plusSeconds(60));

        LearningMilestone undone = service.updateMilestone(card.id(), milestone.id(),
                new LearningMilestoneUpdate("Basics", false, null));
        assertThat(undone.completedAt()).isNull();
    }

    @Test
    void statusInUpdateIsRejectedWhileMilestonesExistAndAcceptedWithoutThem() {
        LearningCard card = service.create(new LearningCardCreate("Kotlin", null, null));
        assertThat(service.update(card.id(), new LearningCardUpdate("Kotlin", null, LearningStatus.IN_PROGRESS))
                .status()).isEqualTo(LearningStatus.IN_PROGRESS);

        service.addMilestone(card.id(), new LearningMilestoneCreate("Basics", null));
        assertThatThrownBy(() -> service.update(card.id(),
                new LearningCardUpdate("Kotlin", null, LearningStatus.COMPLETED)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void milestoneOfAnotherCardIsNotFound() {
        LearningCard card = service.create(new LearningCardCreate("Kotlin", null, null));
        LearningCard other = service.create(new LearningCardCreate("Rust", null, null));
        LearningMilestone milestone = service.addMilestone(other.id(), new LearningMilestoneCreate("Ownership", null));

        assertThatThrownBy(() -> service.updateMilestone(card.id(), milestone.id(),
                new LearningMilestoneUpdate("Ownership", true, null)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deleteMilestone(card.id(), milestone.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void notesAreStampedByTheClockAndDeletedOnlyFromTheirOwnCard() {
        LearningCard card = service.create(new LearningCardCreate("Kotlin", null, null));
        LearningCard other = service.create(new LearningCardCreate("Rust", null, null));

        LearningNote note = service.addNote(card.id(), new LearningNoteCreate("Read chapter 3"));
        assertThat(note.id()).isNotNull();
        assertThat(note.createdAt()).isEqualTo(START);
        assertThat(note.learningCardId()).isEqualTo(card.id());

        assertThatThrownBy(() -> service.deleteNote(other.id(), note.id()))
                .isInstanceOf(NotFoundException.class);

        service.deleteNote(card.id(), note.id());
        assertThat(service.get(card.id()).notes()).isEmpty();
    }

    @Test
    void deletingACardRemovesItsMilestonesAndNotesAndPublishesTheEvent() {
        LearningCard card = service.create(new LearningCardCreate("Kotlin", null, null));
        service.addMilestone(card.id(), new LearningMilestoneCreate("Basics", null));
        service.addNote(card.id(), new LearningNoteCreate("Read chapter 3"));

        service.delete(card.id());

        assertThat(cards.count()).isZero();
        assertThatThrownBy(() -> service.get(card.id())).isInstanceOf(NotFoundException.class);
        assertThat(events.stream(SourceDeletedEvent.class))
                .containsExactly(new SourceDeletedEvent(PlanItemSourceType.LEARNING_RESOURCE, card.id()));
    }

    private LearningStatus status(Long cardId) {
        return service.get(cardId).status();
    }
}
