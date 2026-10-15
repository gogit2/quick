package com.quickflow.learning;

import com.quickflow.common.ConflictException;
import com.quickflow.common.NotFoundException;
import com.quickflow.common.PlanItemSourceType;
import com.quickflow.common.SourceDeletedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for learning cards and their milestones. Time comes from the injected Clock. */
@Service
public class LearningService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final LearningCardRepository cards;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public LearningService(LearningCardRepository cards, Clock clock, ApplicationEventPublisher events) {
        this.cards = cards;
        this.clock = clock;
        this.events = events;
    }

    /** Newest first (D-11), with milestones and notes. */
    @Transactional(readOnly = true)
    public List<LearningCard> list() {
        return cards.findAll(NEWEST_FIRST).stream().map(LearningService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LearningCard get(long id) {
        return toDto(load(id));
    }

    /** A card created without milestones keeps the status given by hand (D-7); none means NOT_STARTED. */
    @Transactional
    public LearningCard create(LearningCardCreate in) {
        LearningStatus status = in.status() == null ? LearningStatus.NOT_STARTED : in.status();
        LearningCardEntity entity = new LearningCardEntity(in.title().trim(), in.description(), status,
                Instant.now(clock));
        return toDto(cards.save(entity));
    }

    /**
     * Title and description are replaced. The status is accepted only while the card has no milestones (D-7, D-16);
     * otherwise it is 409. Without a status the current one is kept.
     */
    @Transactional
    public LearningCard update(long id, LearningCardUpdate in) {
        LearningCardEntity entity = load(id);
        if (in.status() != null && !entity.getMilestones().isEmpty()) {
            throw new ConflictException("Status follows the milestones while the card has any");
        }
        entity.setTitle(in.title().trim());
        entity.setDescription(in.description());
        if (in.status() != null) {
            entity.setStatus(in.status());
        }
        return toDto(entity);
    }

    /** Deletes the card with its milestones and notes, and tells the plans so their items are removed (BR14, D-5). */
    @Transactional
    public void delete(long id) {
        LearningCardEntity entity = load(id);
        cards.delete(entity);
        events.publishEvent(new SourceDeletedEvent(PlanItemSourceType.LEARNING_RESOURCE, id));
    }

    /** Adds a milestone; the card status is re-derived (D-7). */
    @Transactional
    public LearningMilestone addMilestone(long cardId, LearningMilestoneCreate in) {
        LearningCardEntity card = load(cardId);
        LearningMilestoneEntity milestone = new LearningMilestoneEntity(in.title().trim(), in.targetDate());
        card.addMilestone(milestone);
        deriveStatus(card);
        cards.flush();
        return toDto(milestone);
    }

    /**
     * Updates a milestone of exactly this card (BR9, otherwise 404). completedAt is set when done turns true and
     * cleared when it turns false; the card status is re-derived (D-7).
     */
    @Transactional
    public LearningMilestone updateMilestone(long cardId, long milestoneId, LearningMilestoneUpdate in) {
        LearningCardEntity card = load(cardId);
        LearningMilestoneEntity milestone = findMilestone(card, milestoneId);
        milestone.setTitle(in.title().trim());
        milestone.setTargetDate(in.targetDate());
        if (in.done() && !milestone.isDone()) {
            milestone.setCompletedAt(Instant.now(clock));
        } else if (!in.done()) {
            milestone.setCompletedAt(null);
        }
        milestone.setDone(in.done());
        deriveStatus(card);
        return toDto(milestone);
    }

    /** Removes a milestone of exactly this card (BR9, otherwise 404); the card status is re-derived (D-7). */
    @Transactional
    public void deleteMilestone(long cardId, long milestoneId) {
        LearningCardEntity card = load(cardId);
        LearningMilestoneEntity milestone = findMilestone(card, milestoneId);
        card.getMilestones().remove(milestone);
        deriveStatus(card);
    }

    /** Adds a note to the card; createdAt comes from the Clock. */
    @Transactional
    public LearningNote addNote(long cardId, LearningNoteCreate in) {
        LearningCardEntity card = load(cardId);
        LearningNoteEntity note = new LearningNoteEntity(in.text(), Instant.now(clock));
        card.addNote(note);
        cards.flush();
        return toDto(note);
    }

    /** Removes a note of exactly this card (otherwise 404). */
    @Transactional
    public void deleteNote(long cardId, long noteId) {
        LearningCardEntity card = load(cardId);
        LearningNoteEntity note = card.getNotes().stream()
                .filter(n -> n.getId() == noteId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Note " + noteId + " not found on learning card " + cardId));
        card.getNotes().remove(note);
    }

    private LearningCardEntity load(long id) {
        return cards.findById(id)
                .orElseThrow(() -> new NotFoundException("Learning card " + id + " not found"));
    }

    private static LearningMilestoneEntity findMilestone(LearningCardEntity card, long milestoneId) {
        return card.getMilestones().stream()
                .filter(m -> m.getId() == milestoneId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Milestone " + milestoneId + " not found on learning card " + card.getId()));
    }

    /** Re-derives the status when the card has milestones; otherwise the status set by hand stays. */
    private static void deriveStatus(LearningCardEntity card) {
        List<Boolean> doneFlags = card.getMilestones().stream().map(LearningMilestoneEntity::isDone).toList();
        Optional<LearningStatus> derived = LearningStatusCalculator.statusFor(doneFlags);
        derived.ifPresent(card::setStatus);
    }

    private static LearningCard toDto(LearningCardEntity entity) {
        return new LearningCard(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getMilestones().stream().map(LearningService::toDto).toList(),
                entity.getNotes().stream().map(LearningService::toDto).toList());
    }

    private static LearningMilestone toDto(LearningMilestoneEntity entity) {
        return new LearningMilestone(
                entity.getId(),
                entity.getCard().getId(),
                entity.getTitle(),
                entity.isDone(),
                entity.getTargetDate(),
                entity.getCompletedAt());
    }

    private static LearningNote toDto(LearningNoteEntity entity) {
        return new LearningNote(
                entity.getId(),
                entity.getCard().getId(),
                entity.getText(),
                entity.getCreatedAt());
    }
}
