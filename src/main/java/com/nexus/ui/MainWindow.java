package com.nexus.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import com.nexus.config.AppContext;
import com.nexus.model.Category;
import com.nexus.ui.components.CategoryDialog;
import com.nexus.ui.components.NotificationBell;
import com.nexus.ui.components.Sidebar;
import com.nexus.ui.views.*;
import com.nexus.viewmodel.*;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Root window of the Nexus application.
 *
 * <p>Layout:
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Nexus                               [🔔 bell]  [🌙/☀ theme]  │
 * ├──────────┬──────────────────────────────────────────────────────┤
 * │ Sidebar  │  Active view (TaskList / Today / Week / Matrix /      │
 * │          │               Pomodoro)          [DetailPanel]        │
 * └──────────┴──────────────────────────────────────────────────────┘
 * </pre>
 */
public class MainWindow extends BorderPane {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private final AppContext appContext;
    private boolean          isDarkTheme = true;

    // ── View models ───────────────────────────────────────────────────────────
    private TaskListViewModel   listVm;
    private TaskDetailViewModel detailVm;
    private TodayViewModel      todayVm;
    private WeekViewModel       weekVm;
    private EisenhowerViewModel eisenhowerVm;
    private PomodoroViewModel   pomodoroVm;
    private GoalsViewModel      goalsVm;
    private DashboardViewModel  dashboardVm;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TaskListView    taskListView;
    private TaskDetailPanel detailPanel;
    private TodayView       todayView;
    private WeekView        weekView;
    private EisenhowerView  eisenhowerView;
    private PomodoroView    pomodoroView;
    private GoalsView       goalsView;
    private DashboardView   dashboardView;

    // ── Components ────────────────────────────────────────────────────────────
    private Sidebar          sidebar;
    private NotificationBell notificationBell;

    public MainWindow(AppContext appContext) {
        this.appContext = appContext;
        build();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        // ViewModels
        listVm       = new TaskListViewModel(appContext.getTaskService(),
                                             appContext.getCategoryService());
        detailVm     = new TaskDetailViewModel(appContext.getTaskService(),
                                               appContext.getCategoryService());
        todayVm      = new TodayViewModel(appContext.getTaskService(),
                                           appContext.getTimeBlockService());
        weekVm       = new WeekViewModel(appContext.getTaskService());
        eisenhowerVm = new EisenhowerViewModel(appContext.getTaskService());
        pomodoroVm   = new PomodoroViewModel(appContext.getPomodoroService(),
                                              appContext.getTaskService());
        goalsVm      = new GoalsViewModel(appContext.getGoalService(),
                                          appContext.getCategoryService());
        dashboardVm  = new DashboardViewModel(appContext.getTaskService(),
                                              appContext.getPomodoroService(),
                                              appContext.getStreakService());

        // Wire notification bell refresh to reminder service
        appContext.getReminderService().setOnNotificationCreated(
            () -> { if (notificationBell != null) notificationBell.refresh(); }
        );

        // Build views
        detailPanel = new TaskDetailPanel(detailVm);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        detailPanel.setOnClose(this::hideDetail);

        taskListView   = new TaskListView(listVm, detailVm, this::showDetail, this::hideDetail);
        todayView      = new TodayView(todayVm);
        weekView       = new WeekView(weekVm);
        eisenhowerView = new EisenhowerView(eisenhowerVm);
        pomodoroView   = new PomodoroView(pomodoroVm);
        goalsView      = new GoalsView(goalsVm);
        dashboardView  = new DashboardView(dashboardVm, appContext.getExportService());

        detailVm.setOnSaved(listVm::loadTasks);

        // Sidebar
        sidebar = new Sidebar(this::onNavigate, this::onAddCategory);
        refreshSidebar();

        // Top bar
        HBox topBar = buildTopBar();

        setTop(topBar);
        setLeft(sidebar);
        setCenter(taskListView);
        setRight(detailPanel);

        getStyleClass().add("main-window");
        loadStylesheets();
    }

    private HBox buildTopBar() {
        Label appLabel = new Label("Nexus");
        appLabel.getStyleClass().add("app-title");
        HBox.setHgrow(appLabel, Priority.ALWAYS);

        notificationBell = new NotificationBell(appContext.getNotificationService());
        notificationBell.refresh();

        ToggleButton themeBtn = new ToggleButton();
        themeBtn.setGraphic(new FontIcon(MaterialDesignW.WEATHER_NIGHT));
        themeBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        themeBtn.setSelected(true);
        themeBtn.setOnAction(e -> toggleTheme(themeBtn));

        HBox topBar = new HBox(8, appLabel, notificationBell, themeBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");
        topBar.setPadding(new Insets(8, 16, 8, 16));
        return topBar;
    }

    // ── Category management ───────────────────────────────────────────────────

    private void onAddCategory() {
        new CategoryDialog().showAndWait().ifPresent(newCategory -> {
            try {
                List<Category> existing = appContext.getCategoryService().getAllCategories();
                int nextPos = existing.stream()
                    .mapToInt(Category::getPosition)
                    .max()
                    .orElse(0) + 1;
                newCategory.setPosition(nextPos);

                appContext.getCategoryService().createCategory(newCategory);
                log.info("Created new life area: {}", newCategory.getName());
                afterCategoryChange();
            } catch (Exception ex) {
                log.error("Failed to create category", ex);
                showErrorAlert("Could not create life area", ex.getMessage());
            }
        });
    }

    private void afterCategoryChange() {
        refreshSidebar();
        listVm.reloadCategories();
        detailVm.reloadCategories();
    }

    private void refreshSidebar() {
        List<Category> categories = appContext.getCategoryService().getAllCategories();
        sidebar.updateCategories(categories);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void onNavigate(Sidebar.NavItem item) {
        hideDetail();
        switch (item) {
            case Sidebar.NavItem.AllTasks() -> {
                listVm.showArchivedProperty().set(false);
                listVm.selectedCategoryProperty().set(null);
                taskListView.setViewTitle("All Tasks");
                showView(taskListView);
            }
            case Sidebar.NavItem.Archive() -> {
                listVm.showArchivedProperty().set(true);
                listVm.selectedCategoryProperty().set(null);
                taskListView.setViewTitle("Archive");
                showView(taskListView);
            }
            case Sidebar.NavItem.ByCategory(var cat) -> {
                listVm.showArchivedProperty().set(false);
                listVm.selectedCategoryProperty().set(cat);
                taskListView.setViewTitle(cat.getName());
                showView(taskListView);
            }
            case Sidebar.NavItem.Today() -> {
                todayVm.reload();
                showView(todayView);
            }
            case Sidebar.NavItem.ThisWeek() -> {
                weekVm.reload();
                showView(weekView);
            }
            case Sidebar.NavItem.Eisenhower() -> {
                eisenhowerVm.reload();
                showView(eisenhowerView);
            }
            case Sidebar.NavItem.Pomodoro() -> showView(pomodoroView);
            case Sidebar.NavItem.Goals() -> {
                goalsVm.reload();
                showView(goalsView);
            }
            case Sidebar.NavItem.Dashboard() -> {
                dashboardView.refresh();
                showView(dashboardView);
            }
        }
    }

    private void showView(javafx.scene.Node view) {
        javafx.scene.Node current = getCenter();
        if (current == view) return;

        if (current != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(100), current);
            fadeOut.setFromValue(current.getOpacity());
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                setCenter(view);
                view.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(160), view);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        } else {
            setCenter(view);
        }
    }

    // ── Detail panel ──────────────────────────────────────────────────────────

    private void showDetail() {
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
    }

    private void hideDetail() {
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        if (getCenter() == taskListView) {
            taskListView.clearSelection();
        }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private void toggleTheme(ToggleButton btn) {
        isDarkTheme = !isDarkTheme;
        if (isDarkTheme) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            btn.setGraphic(new FontIcon(MaterialDesignW.WEATHER_NIGHT));
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            btn.setGraphic(new FontIcon(MaterialDesignW.WEATHER_SUNNY));
        }
        loadStylesheets();
    }

    private void loadStylesheets() {
        getStylesheets().clear();
        var css = getClass().getResource("/css/nexus.css");
        if (css != null) getStylesheets().add(css.toExternalForm());
    }

    private void showErrorAlert(String header, String detail) {
        javafx.scene.control.Alert alert =
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(detail);
        alert.showAndWait();
    }
}
