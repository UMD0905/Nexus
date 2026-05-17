package com.nexus.ui.components;

import atlantafx.base.theme.Styles;
import com.nexus.model.Category;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A modal {@link Dialog} for creating or editing a {@link Category} (life area).
 *
 * <p>Returns the populated {@link Category} on OK, or {@code null} on Cancel.
 * The caller is responsible for persisting via {@code CategoryService}.
 *
 * <p>Icon selection is driven by a curated list of Material Design 2 icons
 * rendered as actual glyphs — users see the icon, not a raw string.
 */
public class CategoryDialog extends Dialog<Category> {

    /** Curated icon map: friendly display name → Ikonli literal. */
    private static final Map<String, String> ICON_OPTIONS = new LinkedHashMap<>();

    static {
        ICON_OPTIONS.put("Briefcase",      "mdi2b-briefcase-outline");
        ICON_OPTIONS.put("Rocket",         "mdi2r-rocket-launch");
        ICON_OPTIONS.put("Boxing Glove",   "mdi2b-boxing-glove");
        ICON_OPTIONS.put("Dumbbell",       "mdi2d-dumbbell");
        ICON_OPTIONS.put("Person",         "mdi2a-account-outline");
        ICON_OPTIONS.put("School / Uni",   "mdi2s-school-outline");
        ICON_OPTIONS.put("Book",           "mdi2b-book-open-page-variant-outline");
        ICON_OPTIONS.put("Pencil",         "mdi2p-pencil-outline");
        ICON_OPTIONS.put("Laptop / Code",  "mdi2l-laptop");
        ICON_OPTIONS.put("Code Braces",    "mdi2c-code-braces");
        ICON_OPTIONS.put("Home",           "mdi2h-home-outline");
        ICON_OPTIONS.put("Heart / Health", "mdi2h-heart-outline");
        ICON_OPTIONS.put("Running",        "mdi2r-run");
        ICON_OPTIONS.put("Bicycle",        "mdi2b-bicycle");
        ICON_OPTIONS.put("Soccer",         "mdi2s-soccer");
        ICON_OPTIONS.put("Music Note",     "mdi2m-music-note");
        ICON_OPTIONS.put("Camera",         "mdi2c-camera-outline");
        ICON_OPTIONS.put("Cash / Finance", "mdi2c-cash");
        ICON_OPTIONS.put("Airplane",       "mdi2a-airplane");
        ICON_OPTIONS.put("Car",            "mdi2c-car-outline");
        ICON_OPTIONS.put("Food",           "mdi2f-food-apple-outline");
        ICON_OPTIONS.put("Palette / Art",  "mdi2p-palette-outline");
        ICON_OPTIONS.put("Star",           "mdi2s-star-outline");
        ICON_OPTIONS.put("Flash / Energy", "mdi2f-flash-outline");
    }

    // ── Form fields ───────────────────────────────────────────────────────────
    private final TextField       nameField   = new TextField();
    private final ColorPicker     colorPicker = new ColorPicker();
    private final ComboBox<String> iconCombo  = new ComboBox<>();

    // Preview dot shown next to the colour picker
    private final Circle previewDot = new Circle(10);

    /**
     * Creates the dialog pre-populated for a new category.
     */
    public CategoryDialog() {
        this(null);
    }

    /**
     * Creates the dialog pre-populated for editing an existing category.
     *
     * @param existing the category to edit; {@code null} = new category mode
     */
    public CategoryDialog(Category existing) {
        setTitle(existing == null ? "New Life Area" : "Edit Life Area");
        setHeaderText(null);
        setResizable(false);

        buildContent(existing);
        buildButtons(existing);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void buildContent(Category existing) {
        // ── Name ──────────────────────────────────────────────────────────────
        nameField.setPromptText("e.g. University, Finance, Music…");
        nameField.setPrefWidth(280);
        if (existing != null) nameField.setText(existing.getName());

        // ── Colour picker ─────────────────────────────────────────────────────
        Color initialColor = existing != null
            ? Color.web(existing.getColor())
            : Color.web("#4A90D9");

        colorPicker.setValue(initialColor);
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        previewDot.setFill(initialColor);
        colorPicker.setOnAction(e -> previewDot.setFill(colorPicker.getValue()));

        HBox colorRow = new HBox(10, colorPicker, previewDot);
        colorRow.setAlignment(Pos.CENTER_LEFT);

        // ── Icon selector ─────────────────────────────────────────────────────
        iconCombo.getItems().addAll(ICON_OPTIONS.keySet());
        iconCombo.setMaxWidth(Double.MAX_VALUE);

        // Render each item as the actual icon glyph + friendly name
        iconCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String displayName, boolean empty) {
                super.updateItem(displayName, empty);
                if (empty || displayName == null) {
                    setGraphic(null); setText(null);
                } else {
                    String ikonLiteral = ICON_OPTIONS.get(displayName);
                    FontIcon icon = new FontIcon(ikonLiteral);
                    icon.setIconSize(18);
                    HBox cell = new HBox(8, icon, new Label(displayName));
                    cell.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(cell);
                    setText(null);
                }
            }
        });
        // Same rendering for the selected-value button cell
        iconCombo.setButtonCell(iconCombo.getCellFactory().call(null));

        // Pre-select icon matching the existing category
        if (existing != null) {
            ICON_OPTIONS.entrySet().stream()
                .filter(e -> e.getValue().equals(existing.getIcon()))
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresentOrElse(
                    iconCombo::setValue,
                    () -> iconCombo.getSelectionModel().selectFirst()
                );
        } else {
            iconCombo.getSelectionModel().selectFirst();
        }

        // ── Layout ────────────────────────────────────────────────────────────
        VBox content = new VBox(14,
            formRow("Area name",   nameField),
            formRow("Colour",      colorRow),
            formRow("Icon",        iconCombo)
        );
        content.setPadding(new Insets(20, 24, 8, 24));

        getDialogPane().setContent(content);
        getDialogPane().getStyleClass().add("category-dialog");

        // Auto-focus the name field
        nameField.requestFocus();
    }

    private void buildButtons(Category existing) {
        ButtonType saveType   = new ButtonType(
            existing == null ? "Create Area" : "Save Changes",
            ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = ButtonType.CANCEL;

        getDialogPane().getButtonTypes().setAll(saveType, cancelType);

        // Style the save button
        Button saveBtn = (Button) getDialogPane().lookupButton(saveType);
        saveBtn.getStyleClass().add(Styles.ACCENT);

        // Disable Save when name is empty
        saveBtn.disableProperty().bind(
            nameField.textProperty().map(s -> s == null || s.isBlank())
        );

        // Result converter: build a Category from the form values on OK
        setResultConverter(btnType -> {
            if (btnType != saveType) return null;

            String hex = toHex(colorPicker.getValue());
            String selectedDisplay = iconCombo.getValue();
            String ikonLiteral = selectedDisplay != null
                ? ICON_OPTIONS.getOrDefault(selectedDisplay, "mdi2s-star-outline")
                : "mdi2s-star-outline";

            Category result = existing != null ? existing : new Category();
            result.setName(nameField.getText().trim());
            result.setColor(hex);
            result.setIcon(ikonLiteral);
            return result;
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox formRow(String labelText, javafx.scene.Node control) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        return new VBox(5, lbl, control);
    }

    /** Converts a JavaFX {@link Color} to a CSS hex string like {@code #3B82F6}. */
    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) Math.round(color.getRed()   * 255),
            (int) Math.round(color.getGreen() * 255),
            (int) Math.round(color.getBlue()  * 255));
    }
}
