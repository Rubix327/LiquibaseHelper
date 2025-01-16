package me.rubix327.liquibasehelper.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import me.rubix327.liquibasehelper.inspection.RulesManager;
import me.rubix327.liquibasehelper.inspection.model.TagRule;
import me.rubix327.liquibasehelper.inspection.model.TagRulesContainer;
import me.rubix327.liquibasehelper.log.MainLogger;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TagCompletionContributor extends CompletionContributor {

    public TagCompletionContributor() {
        if (!StaticSettings.ENABLE_TAG_AUTO_COMPLETION){
            return;
        }

        InsertHandler<LookupElement> insertHandler = new XmlTagInsertHandler();

        for (RulesManager instance : RulesManager.getAllInstances()) {
            MainLogger.info(instance.getProject(), "Registering EntityCompletionContributor...");
            for (TagRulesContainer rulesContainer : instance.getAllRulesContainers()) {
                extend(CompletionType.BASIC,
                        PlatformPatterns.psiElement(XmlTokenType.XML_NAME)
                                .withParent(XmlTag.class)
                                .inside(XmlPatterns.xmlTag().withName(rulesContainer.getParentTagName())),
                        new CompletionProvider<>() {
                            @Override
                            protected void addCompletions(@NotNull CompletionParameters parameters,
                                                          @NotNull ProcessingContext context,
                                                          @NotNull CompletionResultSet result) {

                                if (!StaticSettings.ENABLE_TAG_AUTO_COMPLETION){
                                    return;
                                }
                                if (!parameters.getPosition().getProject().equals(instance.getProject())){
                                    return;
                                }

                                // Получаем текущий тег
                                PsiElement originalPosition = parameters.getOriginalPosition();
                                XmlTag currentTag = PsiTreeUtil.getParentOfType(originalPosition, XmlTag.class);

                                // Получаем теги, которые уже есть внутри родительского
                                Set<String> existingSubTags = new HashSet<>();
                                if (currentTag != null){
                                    existingSubTags = Arrays.stream(currentTag.getSubTags()).map(XmlTag::getName).collect(Collectors.toSet());
                                }

                                MainLogger.info(instance.getProject(), "Applying autocomplete options for tag %s: %s, excluding %s.",
                                        rulesContainer.getParentTagName(), rulesContainer.getTagRules().stream().map(TagRule::getTagName).toList(), existingSubTags);

                                // Добавляем в автокомплит только те теги, которые еще не объявлены
                                for (TagRule tagRule : rulesContainer.getTagRules()) {
                                    if (existingSubTags.contains(tagRule.getTagName())) continue;

                                    result.addElement(LookupElementBuilder
                                            .create(tagRule.getTagName())
                                            .withIcon(AllIcons.Actions.Play_last)
                                            .withTypeText(tagRule.getTagTooltip())
                                            .withInsertHandler(insertHandler)
                                    );

                                }

                                // Глушим встроенные автодополнения
                                result.runRemainingContributors(parameters, resultSet -> {});
                            }
                        }
                );
            }
        }

    }
}
