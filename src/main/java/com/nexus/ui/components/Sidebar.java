package com.nexus.ui.components;

import atlantafx.base.theme.Styles;
import com.nexus.model.Category;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import java.util.List;
import java.util.function.Consumer;

/**
 * The left-side navigation panel.
 *
 * <p>Fixed nav items (All Tasks, Archive) are always visible.
 * Life areas are dynamic — call {@link #updateCategories(List)} after any
 * create/edit/delete to rebuild that section instantly.
 *
 * <p>The {@code +} button next to "Life Areas" fires {@code onAddCategory},
 * letting the parent window open the {@link CategoryDialog}.
 */
public class Sidebar extends VBox {

    // ── Navigation discriminated union ────────────────────────────────────────
    public sealed interface NavItem permits NavItem.AllTasks, NavItem.Archive,
            NavItem.ByCategory, NavItem.Today, NavItem.ThisWeek,
            NavItem.Eisenhower, NavItem.Pomodoro, NavItem.Goals, NavItem.Dashboard {
        record AllTasks()             implements NavItem {}
        record Archive()              implements NavItem {}
        record ByCategory(Category c) implements NavItem {}
        record Today()                implements NavItem {}
        record ThisWeek()             implements NavItem {}
        record Eisenhower()           implements NavItem {}
        record Pomodoro()             implements NavItem {}
        record Goals()                implements NavItem {}
        record Dashboard()            implements NavItem {}
    }

    private final Consumer<NavItem> onNavigate;
    private final Runnable          onAddCategory;   // opens the "New Life Area" dialog
    private       SidebarRow        selectedRow;

    /**
     * @param onNavigate    called when any navigation item is selected
     * @param onAddCategory called when the user clicks the "+" next to Life Areas
     */
    public Sidebar(Consumer<NavItem> onNavigate, Runnable onAddCategory) {
        this.onNavigate    = onNavigate;
        this.onAddCategory = onAddCategory;
        build();
    }

    /**
     * Rebuilds the Life Areas section with fresh category data.
     * Safe to call at any time — existing rows are discarded.
     */
    public void updateCategories(List<Category> categories) {
        getChildren().removeIf(n -> "life-areas".equals(n.getUserData()));

        VBox lifeAreas = buildLifeAreasSection(categories);
        lifeAreas.setUserData("life-areas");
        getChildren().add(lifeAreas);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("sidebar");
        setPrefWidth(220);
        setMinWidth(200);
        setMaxWidth(260);
        setSpacing(0);
        setPadding(new Insets(0, 0, 12, 0));

        // App title
        Label appTitle = new Label("Nexus");
        appTitle.getStyleClass().add("sidebar-title");
        appTitle.setPadding(new Insets(20, 16, 16, 16));

        // Fixed navigation items
        VBox nav = buildSimpleSection("Navigation");
        nav.getChildren().addAll(
            buildNavRow("Dashboard",   new FontIcon(MaterialDesignV.VIEW_DASHBOARD_OUTLINE),
                        new NavItem.Dashboard()),
            buildNavRow("All Tasks",   new FontIcon(MaterialDesignC.CHECKBOX_MARKED_OUTLINE),
                        new NavItem.AllTasks()),
            buildNavRow("Today",       new FontIcon(MaterialDesignC.CALENDAR_TODAY),
                        new NavItem.Today()),
            buildNavRow("This Week",   new FontIcon(MaterialDesignC.CALENDAR_WEEK),
                        new NavItem.ThisWeek()),
            buildNavRow("Goals",       new FontIcon(MaterialDesignF.FLAG_OUTLINE),
                        new NavItem.Goals()),
            buildNavRow("Matrix",      new FontIcon(MaterialDesignV.VIEW_GRID_OUTLINE),
                        new NavItem.Eisenhower()),
            buildNavRow("Pomodoro",    new FontIcon(MaterialDesignT.TIMER_OUTLINE),
                        new NavItem.Pomodoro()),
            buildNavRow("Archive",     new FontIcon(MaterialDesignA.ARCHIVE_OUTLINE),
                        new NavItem.Archive())
        );

        // Life areas section — empty until updateCategories() is called
        VBox lifeAreas = buildLifeAreasSection(List.of());
        lifeAreas.setUserData("life-areas");

        getChildren().addAll(appTitle, nav, lifeAreas);
    }

    /**
     * Builds the "Life Areas" section with a + button in the heading row.
     */
    private VBox buildLifeAreasSection(List<Category> categories) {
        // Heading row: "LIFE AREAS" label + spacer + "+" button
        Label heading = new Label("Life Areas".toUpperCase());
        heading.getStyleClass().add("sidebar-section-label");

        Button addBtn = new Button();
        addBtn.setGraphic(new FontIcon(MaterialDesignP.PLUS));
        addBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, "sidebar-add-btn");
        addBtn.setTooltip(new javafx.scene.control.Tooltip("Add new life area"));
        addBtn.setOnAction(e -> { if (onAddCategory != null) onAddCategory.run(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headingRow = new HBox(4, heading, spacer, addBtn);
        headingRow.setAlignment(Pos.CENTER_LEFT);
        headingRow.setPadding(new Insets(16, 8, 4, 16));

        VBox section = new VBox(0);
        section.getChildren().add(headingRow);

        for (Category cat : categories) {
            section.getChildren().add(buildCategoryRow(cat));
        }

        if (categories.isEmpty()) {
            Label empty = new Label("No areas yet — press +");
            empty.getStyleClass().add("sidebar-section-label");
            empty.setPadding(new Insets(4, 16, 4, 16));
            section.getChildren().add(empty);
        }

        return section;
    }

    /** Builds a section with just a plain heading (no action button). */
    private VBox buildSimpleSection(String heading) {
        Label label = new Label(heading.toUpperCase());
        label.getStyleClass().add("sidebar-section-label");
        label.setPadding(new Insets(16, 16, 4, 16));
        VBox box = new VBox(0);
        box.getChildren().add(label);
        return box;
    }

    private SidebarRow buildNavRow(String text, FontIcon icon, NavItem item) {
        icon.setIconSize(18);
        SidebarRow row = new SidebarRow(icon, text);
        row.setOnMouseClicked(e -> selectRow(row, item));
        return row;
    }

    private SidebarRow buildCategoryRow(Category cat) {
        Circle dot = new Circle(5, Color.web(cat.getColor()));
        SidebarRow row = new SidebarRow(dot, cat.getName());
        row.setOnMouseClicked(e -> selectRow(row, new NavItem.ByCategory(cat)));
        return row;
    }

    private void selectRow(SidebarRow row, NavItem item) {
        if (selectedRow != null) selectedRow.setSelected(false);
        selectedRow = row;
        selectedRow.setSelected(true);
        onNavigate.accept(item);
    }

    // ── Inner cell class ──────────────────────────────────────────────────────

    private static class SidebarRow extends HBox {

        SidebarRow(javafx.scene.Node icon, String text) {
            super(10);
            getStyleClass().add("sidebar-row");
            setPadding(new Insets(8, 16, 8, 16));
            setCursor(Cursor.HAND);

            Label lbl = new Label(text);
            lbl.getStyleClass().add("sidebar-row-label");
            getChildren().addAll(icon, lbl);
        }

        void setSelected(boolean selected) {
            if (selected) {
                if (!getStyleClass().contains("selected")) getStyleClass().add("selected");
            } else {
                getStyleClass().remove("selected");
            }
        }
    }
}
