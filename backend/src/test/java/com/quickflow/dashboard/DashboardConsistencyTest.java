package com.quickflow.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.quickflow.support.MutableClock;
import com.quickflow.support.MutableClockTestConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * FR-09 / SC-005: every dashboard figure equals the value computed from the list endpoints, over seeded data.
 * The class shares one database, so the scenario tests compare figures with their own list reads, not absolute numbers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/dashboard-consistency-${random.uuid}")
class DashboardConsistencyTest {

    private static final Instant NOW = Instant.parse("2026-10-15T09:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setClock() {
        clock.set(NOW);
    }

    @Test
    void everyDashboardFigureEqualsTheListEndpoints() throws Exception {
        LocalDate today = LocalDate.parse(json("/api/dashboard").read("$.date", String.class));
        seedTasks(today);
        seedHabits();
        seedPlans();
        seedLearning();

        DocumentContext dash = json("/api/dashboard");
        LocalDate day = LocalDate.parse(dash.read("$.date", String.class));
        ZoneId zone = ZoneId.of(dash.read("$.timeZone", String.class));

        List<Map<String, Object>> open = rows("/api/tasks?archived=false");
        int total = open.size();
        int completed = count(open, r -> "DONE".equals(r.get("status")));
        int completedToday = count(open, r -> "DONE".equals(r.get("status"))
                && day.equals(LocalDate.ofInstant(Instant.parse((String) r.get("updatedAt")), zone)));
        int percent = total == 0 ? 0 : (2 * completed * 100 + total) / (2 * total);
        int dueToday = size("/api/tasks?due=TODAY");
        int overdue = size("/api/tasks?due=OVERDUE");

        assertThat(dash.<Integer>read("$.tasks.dueToday")).isEqualTo(dueToday);
        assertThat(dash.<Integer>read("$.tasks.overdue")).isEqualTo(overdue);
        assertThat(dash.<Integer>read("$.tasks.completedToday")).isEqualTo(completedToday);
        assertThat(dash.<Integer>read("$.tasks.total")).isEqualTo(total);
        assertThat(dash.<Integer>read("$.tasks.completed")).isEqualTo(completed);
        assertThat(dash.<Integer>read("$.tasks.completionPercent")).isEqualTo(percent);
        assertThat(dash.<List<?>>read("$.todayTasks")).hasSize(dueToday);
        assertThat(dash.<List<?>>read("$.overdueTasks")).hasSize(overdue);
        // archived tasks are excluded everywhere on the dashboard
        assertThat(open).allMatch(r -> Boolean.FALSE.equals(r.get("archived")));
        assertThat(dash.<List<Map<String, Object>>>read("$.todayTasks[*]"))
                .allMatch(r -> Boolean.FALSE.equals(r.get("archived")));

        List<Map<String, Object>> habits = rows("/api/habits?active=true");
        assertThat(dash.<Integer>read("$.habits.active")).isEqualTo(habits.size());
        assertThat(dash.<Integer>read("$.habits.completedToday"))
                .isEqualTo(count(habits, r -> Boolean.TRUE.equals(r.get("doneForCurrentPeriod"))));
        assertThat(dash.<List<?>>read("$.habits.items")).hasSize(habits.size());

        List<Map<String, Object>> plans = rows("/api/plans");
        assertThat(dash.<Integer>read("$.plans.active"))
                .isEqualTo(count(plans, r -> "IN_PROGRESS".equals(r.get("status"))));
        assertThat(dash.<Integer>read("$.plans.upcoming"))
                .isEqualTo(count(plans, r -> "NOT_STARTED".equals(r.get("status"))));
        assertThat(dash.<Integer>read("$.plans.completed"))
                .isEqualTo(count(plans, r -> "COMPLETED".equals(r.get("status"))));
        // /api/plans is already ordered by priorityOrder, then start, which is the dashboard order too
        assertThat(dash.<List<Number>>read("$.plans.items[*].id"))
                .containsExactlyElementsOf(ids(plans.stream()
                        .filter(r -> "IN_PROGRESS".equals(r.get("status"))).toList()));

        List<Map<String, Object>> cards = rows("/api/learning-cards");
        List<Map<String, Object>> milestones = new ArrayList<>();
        cards.forEach(c -> milestones.addAll(milestonesOf(c)));
        assertThat(dash.<Integer>read("$.learning.totalCards")).isEqualTo(cards.size());
        assertThat(dash.<Integer>read("$.learning.notStarted"))
                .isEqualTo(count(cards, r -> "NOT_STARTED".equals(r.get("status"))));
        assertThat(dash.<Integer>read("$.learning.inProgress"))
                .isEqualTo(count(cards, r -> "IN_PROGRESS".equals(r.get("status"))));
        assertThat(dash.<Integer>read("$.learning.completed"))
                .isEqualTo(count(cards, r -> "COMPLETED".equals(r.get("status"))));
        assertThat(dash.<Integer>read("$.learning.milestonesTotal")).isEqualTo(milestones.size());
        assertThat(dash.<Integer>read("$.learning.milestonesDone"))
                .isEqualTo(count(milestones, r -> Boolean.TRUE.equals(r.get("done"))));
        // recentMilestones: done, completed in the last 7 days in the user zone, newest first
        LocalDate earliest = day.minusDays(6);
        List<String> recentTitles = milestones.stream()
                .filter(m -> Boolean.TRUE.equals(m.get("done")) && m.get("completedAt") != null)
                .filter(m -> !LocalDate.ofInstant(Instant.parse((String) m.get("completedAt")), zone).isBefore(earliest))
                .sorted(Comparator.comparing((Map<String, Object> m) -> Instant.parse((String) m.get("completedAt")))
                        .reversed())
                .map(m -> (String) m.get("title"))
                .toList();
        assertThat(dash.<List<String>>read("$.learning.recentMilestones[*].title")).containsExactlyElementsOf(recentTitles);
        assertThat(recentTitles).containsExactly("m-five-days-ago");
    }

    @Test
    void tickingAPlanItemAndCompletingATaskChangeTheDashboardImmediately() throws Exception {
        LocalDate today = LocalDate.parse(json("/api/dashboard").read("$.date", String.class));
        long stepA = createTask("{\"title\":\"step A\",\"dueDate\":\"" + today + "\"}");
        long stepB = createTask("{\"title\":\"step B\"}");
        long planId = createPlan(NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(2)),
                taskItem(stepA) + "," + taskItem(stepB));
        long itemA = itemIdOf(planId, stepA);

        DocumentContext before = json("/api/dashboard");
        int completedBefore = before.read("$.tasks.completed", Integer.class);
        int completedTodayBefore = before.read("$.tasks.completedToday", Integer.class);

        // ticking a task item marks the task DONE and raises the plan's doneItems
        send(put("/api/plans/{p}/items/{i}", planId, itemA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"done\":true}"));
        DocumentContext afterTick = json("/api/dashboard");
        assertThat(afterTick.read("$.tasks.completed", Integer.class)).isEqualTo(completedBefore + 1);
        assertThat(afterTick.read("$.tasks.completedToday", Integer.class)).isEqualTo(completedTodayBefore + 1);
        assertThat(afterTick.<List<Integer>>read(planPath(planId) + ".doneItems")).containsExactly(1);
        assertThat(afterTick.<List<String>>read("$.todayTasks[?(@.id == " + stepA + ")].status"))
                .containsExactly("DONE");

        // completing a task directly does not touch its plan item (BR13 / D-3): the plan keeps its progress
        send(post("/api/tasks/{id}/complete", stepB));
        DocumentContext afterComplete = json("/api/dashboard");
        assertThat(afterComplete.read("$.tasks.completed", Integer.class)).isEqualTo(completedBefore + 2);
        assertThat(afterComplete.<List<Integer>>read(planPath(planId) + ".doneItems")).containsExactly(1);

        // ticking the last item completes the plan, so it leaves the active plans
        send(put("/api/plans/{p}/items/{i}", planId, itemIdOf(planId, stepB)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"done\":true}"));
        DocumentContext afterLastTick = json("/api/dashboard");
        assertThat(afterLastTick.<List<?>>read(planPath(planId))).isEmpty();
    }

    private void seedTasks(LocalDate today) throws Exception {
        createTask("{\"title\":\"due today\",\"dueDate\":\"" + today + "\"}");
        createTask("{\"title\":\"overdue\",\"dueDate\":\"" + today.minusDays(2) + "\"}");
        createTask("{\"title\":\"no due date\"}");
        createTask("{\"title\":\"done today\",\"status\":\"DONE\",\"dueDate\":\"" + today + "\"}");
        long archived = createTask("{\"title\":\"archived\",\"dueDate\":\"" + today + "\"}");
        send(post("/api/tasks/{id}/archive", archived));
    }

    private void seedHabits() throws Exception {
        long read = send(post("/api/habits").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"read\",\"frequency\":\"DAILY\"}")).read("$.id", Long.class);
        long weekly = send(post("/api/habits").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"stretch\",\"frequency\":\"WEEKLY\"}")).read("$.id", Long.class);
        send(post("/api/habits").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"walk\",\"frequency\":\"DAILY\"}"));
        send(post("/api/habits/{id}/completions", read).contentType(MediaType.APPLICATION_JSON).content("{}"));
        send(post("/api/habits/{id}/completions", weekly).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private void seedPlans() throws Exception {
        long inProgressTask = createTask("{\"title\":\"plan task\"}");
        createPlan(NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(1)), taskItem(inProgressTask));
        createPlan(NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)),
                taskItem(createTask("{\"title\":\"tomorrow task\"}")));
        createPlan(NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(1)),
                taskItem(createTask("{\"title\":\"past task\"}")));
    }

    private void seedLearning() throws Exception {
        long card = send(post("/api/learning-cards").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Kotlin\",\"status\":\"IN_PROGRESS\"}")).read("$.id", Long.class);
        long five = milestone(card, "m-five-days-ago");
        long ten = milestone(card, "m-ten-days-ago");
        milestone(card, "m-open");
        completeMilestone(card, five, "m-five-days-ago", NOW.minus(Duration.ofDays(5)));
        completeMilestone(card, ten, "m-ten-days-ago", NOW.minus(Duration.ofDays(10)));
        clock.set(NOW);
        send(post("/api/learning-cards").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Rust\"}"));
    }

    private long milestone(long card, String title) throws Exception {
        return send(post("/api/learning-cards/{id}/milestones", card).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\"}")).read("$.id", Long.class);
    }

    /** Completes a milestone as if on the given day, so completedAt is that day (the Clock stamps it). */
    private void completeMilestone(long card, long milestone, String title, Instant when) throws Exception {
        clock.set(when);
        send(put("/api/learning-cards/{c}/milestones/{m}", card, milestone)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"done\":true}"));
        clock.set(NOW);
    }

    private long createTask(String body) throws Exception {
        return send(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body)).read("$.id", Long.class);
    }

    private long createPlan(Instant start, Instant end, String items) throws Exception {
        return send(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(
                "{\"title\":\"plan\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"" + start
                        + "\",\"endDateTime\":\"" + end + "\",\"priorityOrder\":1,\"items\":[" + items + "]}"))
                .read("$.id", Long.class);
    }

    private static String taskItem(long taskId) {
        return "{\"sourceType\":\"TASK\",\"sourceId\":" + taskId + "}";
    }

    private long itemIdOf(long planId, long taskId) throws Exception {
        List<Number> found = json("/api/plans/" + planId).read("$.items[?(@.sourceId == " + taskId + ")].id");
        return found.get(0).longValue();
    }

    private static String planPath(long planId) {
        return "$.plans.items[?(@.id == " + planId + ")]";
    }

    private int size(String url) throws Exception {
        return rows(url).size();
    }

    private List<Map<String, Object>> rows(String url) throws Exception {
        return json(url).read("$");
    }

    private DocumentContext json(String url) throws Exception {
        return JsonPath.parse(mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private DocumentContext send(MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> milestonesOf(Map<String, Object> card) {
        return (List<Map<String, Object>>) card.get("milestones");
    }

    private static List<Number> ids(List<Map<String, Object>> rows) {
        return rows.stream().map(r -> (Number) r.get("id")).toList();
    }

    private static int count(List<Map<String, Object>> rows, Predicate<Map<String, Object>> test) {
        return (int) rows.stream().filter(test).count();
    }
}
