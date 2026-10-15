package com.quickflow.dashboard;

import com.quickflow.common.UserTime;
import com.quickflow.habit.Habit;
import com.quickflow.habit.HabitService;
import com.quickflow.learning.LearningCard;
import com.quickflow.learning.LearningMilestone;
import com.quickflow.learning.LearningService;
import com.quickflow.learning.LearningStatus;
import com.quickflow.plan.Plan;
import com.quickflow.plan.PlanService;
import com.quickflow.plan.PlanStatus;
import com.quickflow.plan.PlanStatusCalculator;
import com.quickflow.settings.Settings;
import com.quickflow.settings.SettingsService;
import com.quickflow.task.Task;
import com.quickflow.task.TaskDueFilter;
import com.quickflow.task.TaskQuery;
import com.quickflow.task.TaskService;
import com.quickflow.task.TaskStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composes the dashboard from the owning services, so every figure is computed by the same rule as its list
 * endpoint (FR-09). Only the dashboard-specific rules live in DashboardRules.
 */
@Service
public class DashboardService {

    private static final TaskQuery ALL_OPEN = new TaskQuery(null, null, null, null, null, null, false, null);
    private static final TaskQuery DUE_TODAY = new TaskQuery(null, null, null, TaskDueFilter.TODAY,
            null, null, false, null);
    private static final TaskQuery OVERDUE = new TaskQuery(null, null, null, TaskDueFilter.OVERDUE,
            null, null, false, null);

    private final TaskService tasks;
    private final HabitService habits;
    private final PlanService plans;
    private final LearningService learning;
    private final SettingsService settings;
    private final UserTime userTime;

    public DashboardService(TaskService tasks, HabitService habits, PlanService plans, LearningService learning,
                            SettingsService settings, UserTime userTime) {
        this.tasks = tasks;
        this.habits = habits;
        this.plans = plans;
        this.learning = learning;
        this.settings = settings;
        this.userTime = userTime;
    }

    /** One read of every section, all for the same day and zone. */
    @Transactional(readOnly = true)
    public Dashboard get() {
        LocalDate today = userTime.today();
        ZoneId zone = userTime.zone();
        Settings current = settings.get();
        List<Task> dueToday = tasks.list(DUE_TODAY);
        List<Task> overdue = tasks.list(OVERDUE);
        return new Dashboard(
                today,
                current.timeZone(),
                current.displayName(),
                taskMetrics(dueToday, overdue, today, zone),
                dueToday,
                overdue,
                habitSection(),
                planSection(),
                learningSection(today, zone));
    }

    private DashboardTaskMetrics taskMetrics(List<Task> dueToday, List<Task> overdue, LocalDate today, ZoneId zone) {
        List<Task> open = tasks.list(ALL_OPEN);
        int total = open.size();
        int completed = count(open, t -> t.status() == TaskStatus.DONE);
        return new DashboardTaskMetrics(
                dueToday.size(),
                overdue.size(),
                count(open, t -> DashboardRules.completedOn(t, today, zone)),
                total,
                completed,
                PlanStatusCalculator.completionPercent(completed, total));
    }

    private DashboardHabits habitSection() {
        List<Habit> active = habits.list(true);
        return new DashboardHabits(
                active.size(),
                count(active, Habit::doneForCurrentPeriod),
                active);
    }

    private DashboardPlans planSection() {
        List<Plan> all = plans.list(null);
        return new DashboardPlans(
                count(all, p -> p.status() == PlanStatus.IN_PROGRESS),
                count(all, p -> p.status() == PlanStatus.NOT_STARTED),
                count(all, p -> p.status() == PlanStatus.COMPLETED),
                all.stream().filter(p -> p.status() == PlanStatus.IN_PROGRESS).toList());
    }

    private DashboardLearning learningSection(LocalDate today, ZoneId zone) {
        List<LearningCard> cards = learning.list();
        List<LearningMilestone> milestones = cards.stream().flatMap(c -> c.milestones().stream()).toList();
        return new DashboardLearning(
                cards.size(),
                count(cards, c -> c.status() == LearningStatus.NOT_STARTED),
                count(cards, c -> c.status() == LearningStatus.IN_PROGRESS),
                count(cards, c -> c.status() == LearningStatus.COMPLETED),
                count(milestones, LearningMilestone::done),
                milestones.size(),
                DashboardRules.recentMilestones(cards, today, zone));
    }

    private static <T> int count(List<T> items, Predicate<T> test) {
        return (int) items.stream().filter(test).count();
    }
}
