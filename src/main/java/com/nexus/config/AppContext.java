package com.nexus.config;

import com.nexus.repository.*;
import com.nexus.service.*;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Manual dependency-injection container.
 *
 * <p>Creates every object exactly once (singleton scope), wiring dependencies
 * in construction order.  This replaces a DI framework — for a single-process
 * desktop app, the simplicity is worth more than the added magic.
 *
 * <p>Call {@link #shutdown()} when the application exits to close the
 * HikariCP connection pool gracefully.
 */
public class AppContext {

    private static final Logger log = LoggerFactory.getLogger(AppContext.class);

    private static volatile AppContext instance;

    // ── Infrastructure ────────────────────────────────────────────────────────
    private final DataSource  dataSource;
    private final DSLContext  dsl;

    // ── Repositories ──────────────────────────────────────────────────────────
    private final TaskRepository            taskRepository;
    private final CategoryRepository        categoryRepository;
    private final ProjectRepository         projectRepository;
    private final TagRepository             tagRepository;
    private final SubtaskRepository         subtaskRepository;
    private final NotificationRepository    notificationRepository;
    private final RecurrenceRuleRepository  recurrenceRuleRepository;
    private final TimeBlockRepository       timeBlockRepository;
    private final PomodoroSessionRepository pomodoroSessionRepository;
    private final GoalRepository            goalRepository;
    private final StreakRepository          streakRepository;

    // ── Services ──────────────────────────────────────────────────────────────
    private final TaskService         taskService;
    private final CategoryService     categoryService;
    private final ProjectService      projectService;
    private final NotificationService notificationService;
    private final RecurrenceService   recurrenceService;
    private final TimeBlockService    timeBlockService;
    private final PomodoroService     pomodoroService;
    private final ReminderService     reminderService;
    private final GoalService         goalService;
    private final StreakService       streakService;
    private final ExportService       exportService;

    // ── Private constructor ───────────────────────────────────────────────────
    private AppContext() {
        log.info("Initialising Nexus application context...");

        // 1. Database
        this.dataSource = DatabaseConfig.createDataSource();
        DatabaseConfig.runMigrations(dataSource);
        this.dsl        = JooqConfig.createDslContext(dataSource);

        // 2. Repositories
        this.taskRepository            = new TaskRepository(dsl);
        this.categoryRepository        = new CategoryRepository(dsl);
        this.projectRepository         = new ProjectRepository(dsl);
        this.tagRepository             = new TagRepository(dsl);
        this.subtaskRepository         = new SubtaskRepository(dsl);
        this.notificationRepository    = new NotificationRepository(dsl);
        this.recurrenceRuleRepository  = new RecurrenceRuleRepository(dsl);
        this.timeBlockRepository       = new TimeBlockRepository(dsl);
        this.pomodoroSessionRepository = new PomodoroSessionRepository(dsl);
        this.goalRepository            = new GoalRepository(dsl);
        this.streakRepository          = new StreakRepository(dsl);

        // 3. Services (order matters — TaskService needs StreakService injected after)
        this.notificationService = new NotificationService(notificationRepository);
        this.categoryService     = new CategoryService(categoryRepository);
        this.projectService      = new ProjectService(projectRepository, categoryRepository);
        this.streakService       = new StreakService(streakRepository, categoryRepository);
        this.taskService         = new TaskService(taskRepository, categoryRepository,
                                                   tagRepository, subtaskRepository,
                                                   streakService);
        this.recurrenceService   = new RecurrenceService(recurrenceRuleRepository, taskRepository);
        this.timeBlockService    = new TimeBlockService(timeBlockRepository);
        this.pomodoroService     = new PomodoroService(pomodoroSessionRepository, taskRepository);
        this.reminderService     = new ReminderService(taskRepository, notificationService);
        this.goalService         = new GoalService(goalRepository, taskRepository, categoryRepository);
        this.exportService       = new ExportService(taskRepository, categoryRepository,
                                                     tagRepository, goalRepository);

        // 4. Generate recurring task instances for the next 14 days
        try {
            int generated = recurrenceService.generateUpcoming(14);
            log.info("Startup recurrence generation: {} new instance(s)", generated);
        } catch (Exception e) {
            log.warn("Recurrence generation failed (non-fatal): {}", e.getMessage());
        }

        // 5. Expire stale streaks (reset current streak if not completed yesterday)
        try {
            streakService.expireStaleStreaks();
        } catch (Exception e) {
            log.warn("Streak expiry check failed (non-fatal): {}", e.getMessage());
        }

        // 6. Start background reminder scheduler
        reminderService.start();

        log.info("Application context ready.");
    }

    public static AppContext getInstance() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) {
                    instance = new AppContext();
                }
            }
        }
        return instance;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public TaskService            getTaskService()            { return taskService; }
    public CategoryService        getCategoryService()        { return categoryService; }
    public ProjectService         getProjectService()         { return projectService; }
    public NotificationService    getNotificationService()    { return notificationService; }
    public RecurrenceService      getRecurrenceService()      { return recurrenceService; }
    public TimeBlockService       getTimeBlockService()       { return timeBlockService; }
    public PomodoroService        getPomodoroService()        { return pomodoroService; }
    public ReminderService        getReminderService()        { return reminderService; }
    public GoalService            getGoalService()            { return goalService; }
    public StreakService          getStreakService()           { return streakService; }
    public ExportService          getExportService()          { return exportService; }
    public NotificationRepository getNotificationRepository() { return notificationRepository; }
    public DSLContext             getDsl()                    { return dsl; }

    public void shutdown() {
        log.info("Shutting down application context...");
        reminderService.shutdown();
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hds) {
            hds.close();
            log.info("Connection pool closed.");
        }
    }
}
