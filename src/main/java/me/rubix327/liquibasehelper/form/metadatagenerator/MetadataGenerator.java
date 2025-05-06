package me.rubix327.liquibasehelper.form.metadatagenerator;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.PsiClass;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import me.rubix327.liquibasehelper.AnnotationUtils;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.form.LJBCheckBox;
import me.rubix327.liquibasehelper.inspection.RulesManager;
import me.rubix327.liquibasehelper.inspection.model.AvailableValue;
import me.rubix327.liquibasehelper.inspection.model.TagRule;
import me.rubix327.liquibasehelper.inspection.model.TagRulesContainer;
import me.rubix327.liquibasehelper.locale.DeclinationHelper;
import me.rubix327.liquibasehelper.locale.Localization;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;
import java.util.*;

import static me.rubix327.liquibasehelper.Utils.createTitledBorder;
import static me.rubix327.liquibasehelper.form.metadatagenerator.MetadataGeneratorUtils.convertMapToRow;
import static me.rubix327.liquibasehelper.form.metadatagenerator.MetadataGeneratorUtils.sortRules;

public class MetadataGenerator extends JFrame {

    private final Project project;
    private LJBCheckBox addEmptyTagsCheckbox; // Чекбокс добавления тегов для незаполненных полей
    private LJBCheckBox addCommentsCheckbox; // Чекбокс добавления комментариев над тегами
    private JBTextArea previewArea; // Область просмотра результата

    // Номер в иерархии окон. 1 - родительское (открытое после выбора тега в MetadataGeneratorChooseDialog)
    private int parentalIndex = 1;
    // Тег, для которого составляются метаданные в данном окне
    private String tagName;
    // Комментарий к тегу, для которого составляются метаданные
    private String tagTooltip;
    // Контейнер с правилами для тега, для которого составляются метаданные
    private final TagRulesContainer mainContainer;

    // Значения обычных тегов
    private final Map<TagRule, String> tagNamesToValues = new LinkedHashMap<>();
    // Значения вложенных таблиц. Ключ - правило таблицы, значение - список строк таблицы
    private final Map<TagRule, List<Map<TagRule, Object>>> tableValues = new LinkedHashMap<>();
    // Хранение всех лейблов окна, нужно для придания одинакового размера всем лейблам
    private final List<JBLabel> allLabels = new ArrayList<>();
    // Заранее определенные данные (нужны для редактирования строки при открытии дочернего окна (с parentalIndex > 1))
    private Map<TagRule, ?> predefinedData = new LinkedHashMap<>();
    // Обработчик кликов (в частности при клике ОК на окне)
    private ClickListener clickListener;
    // Окна, открытые изнутри данного
    private final List<MetadataGenerator> children = new ArrayList<>();

    public MetadataGenerator(@NotNull Project project, @NotNull String tagName){
        this(project, tagName, null, 1, null);
    }

    public MetadataGenerator(@NotNull Project project, @NotNull String tagName, @Nullable Map<TagRule, ?> predefinedData,
                             int parentalIndex, @Nullable ClickListener clickListener){
        this.project = project;
        this.mainContainer = RulesManager.getInstance(project).getRulesContainerByTagName(tagName);
        if (this.mainContainer == null || this.mainContainer.getTagRules() == null){
            this.setVisible(false);
            return;
        }

        this.tagName = this.mainContainer.getParentTagName();
        this.tagTooltip = this.mainContainer.getParentTagTooltip();
        this.clickListener = clickListener;
        this.parentalIndex = parentalIndex;
        this.predefinedData = predefinedData == null ? new LinkedHashMap<>() : predefinedData;

        System.out.println("Got predefinedData map:");
        for (Map.Entry<TagRule, ?> entry : this.predefinedData.entrySet()) {
            System.out.println("    Tag " + entry.getKey().getTagName() + " --> " + entry.getValue());
        }

        init();
    }

    private boolean isMain(){
        return this.parentalIndex == 1;
    }

    private LinkedHashMap<TagRule, Object> build(){
        LinkedHashMap<TagRule, Object> map = new LinkedHashMap<>();
        map.putAll(tagNamesToValues);
        map.putAll(tableValues);
        System.out.println("Built map:");
        for (Map.Entry<TagRule, Object> entry : map.entrySet()) {
            System.out.println("   [0] " + entry.getKey().getTagName() + " --> " + entry.getValue());
        }
        for (Map.Entry<TagRule, String> entry : tagNamesToValues.entrySet()) {
            System.out.println("        [1] " + entry.getKey().getTagName() + " --> " + entry.getValue());
        }
        for (Map.Entry<TagRule, List<Map<TagRule, Object>>> entry : tableValues.entrySet()) {
            System.out.println("        [2] " + entry.getKey().getTagName() + " --> " + entry.getValue());
        }
        return map;
    }

    public MetadataGenerator showWindow(){
        pack();
        setVisible(true);
        return this;
    }

    private void init(){
        // Создаем основное окно
        setTitle(Localization.message("metadata-generator.window.title", parentalIndex, tagName));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 800);
        setMinimumSize(new Dimension(400, 400));
        setOpacity(1);
        setBackground(JBColor.WHITE);
        setIconImage(new ImageIcon("D:/IdeaProjects/LiquibaseNavigator/src/main/resources/img/liquibase.svg").getImage()); // TODO починить иконку

        // Основной контейнер с вертикальным BoxLayout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        ///////////////

        addEmptyTagsCheckbox = new LJBCheckBox(Localization.message("metadata-generator.settings.addEmptyTags"));
        addEmptyTagsCheckbox.addLChangeListener(e -> updatePreview());
        addEmptyTagsCheckbox.setBorder(JBUI.Borders.empty(5));
        addCommentsCheckbox = new LJBCheckBox(Localization.message("metadata-generator.settings.addComments"));
        addCommentsCheckbox.addLChangeListener(e -> updatePreview());
        addCommentsCheckbox.setBorder(JBUI.Borders.empty(5));

        ///////////////

        JPanel settingsInnerPanel = new JPanel(new GridLayout(2, 1));
        settingsInnerPanel.add(addEmptyTagsCheckbox);
        settingsInnerPanel.add(addCommentsCheckbox);

        JPanel settingsPanel = new JPanel(new GridLayout(1, 1));
        settingsPanel.setBorder(createTitledBorder(Localization.message("metadata-generator.settings.title")));
        settingsPanel.add(settingsInnerPanel);

        ///////////////

        previewArea = new JBTextArea();
        previewArea.setMinimumSize(new Dimension(400, 100));
        previewArea.setBorder(JBUI.Borders.empty(5));
        previewArea.setEditable(false);
        previewArea.setOpaque(false);
        previewArea.setLineWrap(true);

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setMinimumSize(new Dimension(400, 100));
        previewPanel.setBorder(createTitledBorder(Localization.message("metadata-generator.preview.title")));
        previewPanel.add(previewArea);

        ///////////////

        List<TagRule> basicRules = new ArrayList<>(this.mainContainer.getTagRules().stream().filter(e -> StringUtils.isBlank(e.getListLinkToBaseClass())).toList());
        List<TagRule> tableRules = new ArrayList<>(this.mainContainer.getTagRules().stream().filter(e -> StringUtils.isNotBlank(e.getListLinkToBaseClass())).toList());

        ///////////////

        JPanel tagsPanel = null;
        if (!basicRules.isEmpty()){
            tagsPanel = new JPanel();
            tagsPanel.setLayout(new GridLayout(basicRules.size(), 1));
            tagsPanel.setBorder(createTitledBorder(Localization.message("metadata-generator.tags.title")));
            sortRules(basicRules);

            for (TagRule tagRule : basicRules) {
                tagNamesToValues.put(tagRule, null);
                Object predefinedValue = predefinedData.get(tagRule);
                if (predefinedValue instanceof String s){
                    tagNamesToValues.put(tagRule, s);
                }

                JPanel panel;
                if (Utils.isNotEmpty(tagRule.getAvailableValues())){
                    panel = createPanelWithChooseField(tagRule, tagRule.getAvailableValues());
                } else if (Boolean.class.getTypeName().equals(tagRule.getType())){
                    panel = createPanelWithChooseField(tagRule, AvailableValue.makeYesNoAvailableValues());
                } else {
                    panel = createPanelWithTextField(tagRule);
                }
                tagsPanel.add(panel);
            }

            // Максимальная длина из всех лейблов
            int maxWidth = allLabels.stream().map(l -> l.getPreferredSize().getWidth())
                    .mapToInt(v -> Math.toIntExact(Math.round(v)))
                    .max().orElseThrow(NoSuchElementException::new);

            // Устанавливаем всем лейблам одинаковую длину, чтобы textField были ровно в столбик
            for (JBLabel lab : allLabels) {
                lab.setPreferredSize(new Dimension(maxWidth, 35));
            }
        }

        ///////////////

        JPanel compoundTagsPanel = null;
        if (!tableRules.isEmpty()){
            compoundTagsPanel = new JPanel();
            compoundTagsPanel.setLayout(new GridLayout(tableRules.size(), 1));
            compoundTagsPanel.setBorder(createTitledBorder(Localization.message("metadata-generator.compound-tags.title")));

            for (TagRule tagRule : tableRules){
                JPanel tablePanel = createPanelWithTable(tagRule);
                if (tablePanel != null){
                    compoundTagsPanel.add(tablePanel);
                }
            }
        }

        ///////////////

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton(Localization.message("common.ok"));
        JRootPane rootPane = getRootPane();
        if (rootPane != null){
            rootPane.setDefaultButton(okButton);
        }

        JButton cancelButton = new JButton(Localization.message("common.cancel"));

        // Добавляем действия для кнопок
        okButton.addActionListener(e -> {
            if (clickListener != null){
                clickListener.onOkClicked(build());
            }
            if (isMain()){
                Utils.copyToClipboard(previewArea.getText());
            }
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        ///////////////

        if (tagsPanel != null && tagsPanel.getComponents().length != 0){
            mainPanel.add(tagsPanel, BorderLayout.WEST);
            mainPanel.add(Box.createRigidArea(new Dimension(1, 10)), BorderLayout.WEST);
        }
        if (compoundTagsPanel != null && compoundTagsPanel.getComponents().length != 0){
            mainPanel.add(compoundTagsPanel, BorderLayout.WEST);
            mainPanel.add(Box.createRigidArea(new Dimension(1, 10)), BorderLayout.WEST);
        }
        if (isMain()){
            mainPanel.add(settingsPanel, BorderLayout.WEST);
            mainPanel.add(Box.createRigidArea(new Dimension(1, 10)), BorderLayout.WEST);
            mainPanel.add(previewPanel, BorderLayout.WEST);
        } else {
            mainPanel.add(new JPanel(new BorderLayout()));
        }
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Добавляем отступы между панелями и границами окна
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Добавляем JScrollPane для прокрутки
        JScrollPane scrollPane = new JBScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Добавляем JScrollPane в окно
        add(scrollPane);
    }

    /// PANELS

    private JPanel createPanelWithLabel(TagRule tagRule){
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(0, 32)); // Фиксированная высота панели
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32)); // Не растягивается по высоте
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 10, 2, 10), // Отступы внутри каждой панели
                BorderFactory.createEmptyBorder() // Граница
        ));
        panel.setLayout(new BorderLayout(10, 5)); // BorderLayout с отступами

        String tagLabel = Utils.getTagLabel(tagRule);
        String tagTooltip = Utils.getTagTooltipHtml(tagRule);

        JBLabel label = new JBLabel(tagLabel);
        label.setToolTipText(tagTooltip);
        panel.add(label, BorderLayout.WEST);
        allLabels.add(label);

        return panel;
    }

    private JPanel createPanelWithTextField(TagRule tagRule) {
        JPanel panel = createPanelWithLabel(tagRule);
        JBTextField textField = new JBTextField();
        Border defaultBorder = textField.getBorder();

        // Установка из заранее переданных данных
        String predefinedValue = tagNamesToValues.get(tagRule);
        if (predefinedValue != null){
            textField.setText(predefinedValue);
        }

        validateInput(textField, tagRule, defaultBorder);
        textField.getDocument().addDocumentListener(MetadataGeneratorUtils.getTextFieldListener(() -> {
            tagNamesToValues.put(tagRule, textField.getText());
            updatePreview();
            validateInput(textField, tagRule, defaultBorder);
        }));

        panel.add(textField, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPanelWithChooseField(TagRule tagRule, List<AvailableValue> availableValues){
        JPanel panel = createPanelWithLabel(tagRule);
        List<AvailableValue> availableValueList = new ArrayList<>(availableValues);
        availableValueList.add(0, null); // null-элемент для пустой строки

        DefaultComboBoxModel<AvailableValue> model = new DefaultComboBoxModel<>(availableValueList.toArray(new AvailableValue[0]));
        ComboBox<AvailableValue> comboBox = new ComboBox<>(model);

        comboBox.addActionListener(e -> {
            AvailableValue selectedItem = (AvailableValue) comboBox.getSelectedItem();
            tagNamesToValues.put(tagRule, selectedItem == null ? null : selectedItem.getValue());
            updatePreview();
        });

        comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JBLabel availableValueLabel = new JBLabel();
            if (value != null) {
                if (value.getComment() != null){
                    availableValueLabel.setText(value.getValueAndComment());
                } else {
                    availableValueLabel.setText(value.getValue());
                }
            }
            return availableValueLabel;
        });

        // Установка из заранее переданных данных
        for (AvailableValue availableValue : availableValues) {
            if (availableValue.getValue().equals(tagNamesToValues.get(tagRule))){
                comboBox.setSelectedItem(availableValue);
                break;
            }
        }

        panel.add(comboBox, BorderLayout.CENTER);
        return panel;
    }

    @Nullable
    private JPanel createPanelWithTable(TagRule parentTagRule){
        PsiClass linkedClass = Utils.findPsiClassByQualifiedName(project, parentTagRule.getListLinkToBaseClass());
        if (linkedClass == null || linkedClass.getQualifiedName() == null || AnnotationUtils.isNotDatamodelClass(linkedClass)){
            return null;
        }

        String tableTagName = RulesManager.getDatamodelTagOfClass(linkedClass);
        if (tableTagName == null) return null;


        TagRulesContainer container = RulesManager.getInstance(project).getRulesContainerByTagName(tableTagName);
        List<TagRule> rules = new ArrayList<>(container.getTagRules());
        sortRules(rules);

        MetadataGeneratorTable table = new MetadataGeneratorTable(project, parentTagRule, tableTagName, rules);
        MetadataGeneratorTable.MakeActionRequest request = new MetadataGeneratorTable.MakeActionRequest(
                tableValues, this::updatePreview, this.parentalIndex, this.children
        );

        table.makeAddAction(request);
        table.makeRemoveAction(request);
        table.makeEditAction(request);

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10), // Отступы внутри каждой панели
                createTitledBorder(Utils.getTagLabel(parentTagRule)) // Граница
        ));
        panel.setLayout(new BorderLayout(10, 5)); // BorderLayout с отступами

        // Установка из заранее переданных данных
        Object predefinedTableData = predefinedData.get(parentTagRule);
        if (predefinedTableData instanceof List lst){
            for (Object object : lst) {
                if (object instanceof LinkedHashMap<?, ?> lstMap){
                    table.getModel().addRow(convertMapToRow(lstMap));
                }
                tableValues.put(parentTagRule, lst);
            }
        }

        panel.add(table.getPanel());
        return panel;
    }

    /// PREVIEW

    private void updatePreview(){
        if (!isMain()){
            return;
        }

        StringBuilder sb = new StringBuilder();
        addComment(sb, tagTooltip, 0);
        sb.append("<").append(tagName).append(">\n");

        boolean hasAtLeastOneTag = false;
        for (Map.Entry<TagRule, String> entry : tagNamesToValues.entrySet()) {
            String tag = entry.getKey().getTagName();
            String value = entry.getValue();

            String innerResult = null;
            if ((value != null && !value.isBlank())){
                innerResult = "    <{tag}>{value}</{tag}>";
            } else if (addEmptyTagsCheckbox.isSelected()){
                innerResult = "    <{tag}/>";
            }

            if (innerResult != null){

                addComment(sb, entry.getKey().getTagTooltip(), 1);
                sb.append(innerResult
                        .replaceAll("\\{tag}", tag != null ? tag : "")
                        .replaceAll("\\{value}", value != null && !value.isBlank() ? value : "")
                ).append("\n");

                hasAtLeastOneTag = true;
            }
        }

        for (Map.Entry<TagRule, List<Map<TagRule, Object>>> entry : tableValues.entrySet()) {
            List<Map<TagRule, Object>> value = entry.getValue();
            StringBuilder innerSb = new StringBuilder();
            makePreviewPartFromList(1, value, entry.getKey(), innerSb);

            if (!innerSb.isEmpty()){
                sb.append(innerSb);
                hasAtLeastOneTag = true;
            }
        }

        if (hasAtLeastOneTag){
            sb.append("</").append(tagName).append(">");
            previewArea.setText(sb.toString());
        } else {
            previewArea.setText(null);
        }
    }

    private void makePreviewPartFromList(int offset, List<?> list, TagRule parentTagRule, StringBuilder sb){
        String parentTagName = parentTagRule.getTagName();
        if ((list != null && !list.isEmpty())){
            for (Object object : list) {
                addComment(sb, parentTagRule.getTagTooltip(), offset);
                sb.append(getOffsetStr(offset)).append("<").append(parentTagName).append(">\n");
                if (object instanceof Map<?, ?> m && !m.isEmpty()){
                    for (Map.Entry<?, ?> mapEntry : m.entrySet()) {
                        if (!(mapEntry.getKey() instanceof TagRule tagRule)) continue;
                        if (mapEntry.getValue() instanceof List<?> lst){
                            makePreviewPartFromList(offset + 1, lst, tagRule, sb);
                        } else if (mapEntry.getValue() instanceof String s) {
                            if (Utils.isNotBlank(s)){
                                addComment(sb, tagRule.getTagTooltip(), offset + 1);
                                sb.append(getOffsetStr(offset + 1))
                                        .append("<").append(tagRule.getTagName()).append(">")
                                        .append(mapEntry.getValue())
                                        .append("</").append(tagRule.getTagName()).append(">\n");
                            } else if (addEmptyTagsCheckbox.isSelected()){
                                addComment(sb, tagRule.getTagTooltip(), offset + 1);
                                sb.append(getOffsetStr(offset + 1)).append("<").append(tagRule.getTagName()).append("/>\n");
                            }
                        }
                    }
                }
                sb.append(getOffsetStr(offset)).append("</").append(parentTagName).append(">\n");
            }
        } else if (addEmptyTagsCheckbox.isSelected()){
            sb.append(getOffsetStr(offset)).append("<").append(parentTagName).append("/>\n");
        }
    }

    private void addComment(StringBuilder sb, String comment, int offset){
        if (addCommentsCheckbox.isSelected() && Utils.isNotBlank(comment)){
            sb.append(getOffsetStr(offset)).append("<!-- ").append(comment).append(" -->\n");
        }
    }

    private String getOffsetStr(int offset){
        return "    ".repeat(offset);
    }


    /// MISC

    private void validateInput(JBTextField textField, TagRule tagRule, Border defaultBorder){
        StringBuilder builder = new StringBuilder();

        if (tagRule.getMaxLength() > 0 && textField.getText().length() > tagRule.getMaxLength()) {
            builder.append(DeclinationHelper.CHARACTER_NOMINATIVE_TAG.message(tagRule.getMaxLength()));
        }
        if (tagRule.isRequired() && textField.getText().isBlank()) {
            builder.append(builder.isEmpty() ? "" : "<br>").append(Localization.message("tag.warn.required"));
        }

        if (!builder.isEmpty()){
            textField.setToolTipText(builder.toString());
            textField.setBorder(new LineBorder(JBColor.RED, 1));
        } else {
            textField.setBorder(defaultBorder);
            textField.setToolTipText(null);
        }
    }

    public void dispose(){
        for (MetadataGenerator child : children) {
            if (child != null && child.isVisible()){
                child.dispose();
            }
        }
        super.dispose();
    }

    public interface ClickListener{
        void onOkClicked(LinkedHashMap<TagRule, Object> builtMap);
    }

}
