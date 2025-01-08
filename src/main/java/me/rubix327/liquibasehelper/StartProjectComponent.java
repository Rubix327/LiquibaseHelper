package me.rubix327.liquibasehelper;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Query;
import me.rubix327.liquibasehelper.inspection.RulesManager;
import me.rubix327.liquibasehelper.inspection.model.HandleClassesResponse;
import me.rubix327.liquibasehelper.listener.ClassDeletionListener;
import me.rubix327.liquibasehelper.log.MainLogger;
import me.rubix327.liquibasehelper.settings.PersistentUserSettings;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static me.rubix327.liquibasehelper.inspection.model.HandleClassesResponse.ErrorReason.*;

public class StartProjectComponent implements ProjectComponent {

    private static final Map<String, String> projectPathToArtifactId = new HashMap<>();
    private final Project project;

    public StartProjectComponent(Project project) {
        this.project = project;
    }

    public static String getArtifactId(Project project){
        return projectPathToArtifactId.get(project.getBasePath());
    }

    @Override
    public void projectOpened() {
        MainLogger.info(project, "Loading settings:");
        PersistentUserSettings settings = project.getService(PersistentUserSettings.class);
        for (String s : settings.toString().split("\n")) {
            MainLogger.info(project, 1, s);
        }

        registerClassDeletionListener(project);

        if (projectPathToArtifactId.get(project.getBasePath()) == null){
            VirtualFile file = Utils.findFileInProject(project, "pom.xml", "!src", "!metaloader", "!datamodel");
            MainLogger.info(project, "Found pom.xml file: %s", file);

            if (file != null){
                String artifactId = getArtifactId(file);
                projectPathToArtifactId.put(project.getBasePath(), artifactId);
                MainLogger.info(project, "Found pom.xml artifactId: %s", artifactId);
            }
        }

        // Проверяем, что проект уже в режиме "умной" работы (индексация завершена)
        if (!DumbService.getInstance(project).isDumb()) {
            MainLogger.info(project, "The project is in smart mode.");
            registerRulesForAllClasses(project);
        } else {
            // Если индексация еще не завершена, регистрируем слушателя
            MainLogger.info(project, "The project is in dumb mode. Rules will be registered later.");
            DumbService.getInstance(project).runWhenSmart(() -> registerRulesForAllClasses(project));
        }

    }

    public static void registerRulesForAllClasses(Project project){
        RulesManager rulesManagerInstance = RulesManager.getInstance(project);
        rulesManagerInstance.resetAll();
        Query<PsiClass> allClasses = AllClassesSearch.search(GlobalSearchScope.projectScope(project), project);

        List<HandleClassesResponse> skippedResponses = new ArrayList<>();
        MainLogger.info(project, "Registering project-level rules...");
        MainLogger.info(project, 1, "Registered rules for classes:");
        for (PsiClass psiClass : allClasses) {
            HandleClassesResponse response = rulesManagerInstance.handleClassAndSuperClasses(psiClass);
            if (response.isSuccess()){
                MainLogger.info(project, 2, response.getMessage());
            } else {
                skippedResponses.add(response);
            }
        }

        if (!skippedResponses.isEmpty()){
            MainLogger.info(project, 1, "Skipped classes (%s):", skippedResponses.size());
        }
        logSkippedClasses(project, skippedResponses, CLASS_IS_NOT_DATAMODEL, "- Not datamodel classes ({count}): {classes}");
        logSkippedClasses(project, skippedResponses, CLASS_IS_MAPPED, "- Mapped classes ({count}): {classes}");
        logSkippedClasses(project, skippedResponses, CLASS_IS_INNER, "- Inner classes ({count}): {classes}");
        logSkippedClasses(project, skippedResponses, CANNOT_GET_QUALIFIED_NAME, "- Could not get qualified name ({count}): {classes}");
        logSkippedClasses(project, skippedResponses, CANNOT_GET_DATAMODEL_TAG, "- Could not get datamodel tag ({count}): {classes}");

        ClassDeletionListener.unregisterProjectRulesUpdate(project);
        MainLogger.info(project, "Project-level rules have been registered.");

        registerRulesFromDependencies(rulesManagerInstance);
        rulesManagerInstance.printAllRules();
    }

    private static void logSkippedClasses(Project project, List<HandleClassesResponse> responses, HandleClassesResponse.ErrorReason errorReason, String msg){
        Set<String> skipped = responses.stream()
                .filter(r -> r.getErrorReason() == errorReason)
                .map(r -> r.getBaseClass().getName())
                .collect(Collectors.toSet());
        if (Utils.isNotEmpty(skipped)){
            MainLogger.info(project, 2, msg
                    .replace("{count}", String.valueOf(skipped.size()))
                    .replace("{classes}", String.valueOf(skipped))
            );
        }
    }

    public static void registerRulesFromDependencies(RulesManager rulesManager){
        Project project = rulesManager.getProject();
        try{
            MainLogger.info(project, "Registering rules from dependencies...");
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);

            // TODO вынести в настройки
            List<String> additionalPackages = List.of(
                    // cbscoreservices
                    "ru.athena.cbs.base.metaloader.metaentity",
                    "ru.athena.cbs.base.metaloader.metaentity.addtitionalattributes",
                    "ru.athena.cbs.coreservices.metaloader.entitylabels.metaentity",
                    "ru.athena.cbs.coreservices.metaloader.enumeration.metaentity",
                    "ru.athena.cbs.coreservices.metaloader.externaldocid.metaentity",
                    "ru.athena.cbs.coreservices.metaloader.metaentity",
                    "ru.athena.cbs.coreservices.metaloader.registrykey.metaentity",
                    "ru.athena.cbs.coreservices.metaloader.userkey.metaentity",
                    // cbsdocengine
                    "ru.athena.cbs.docengine.metaloader.metaentity",
                    // cbsdocnumber
                    "ru.athena.cbs.cbsdocnumber.metaloader.metaentity"
            );

            for (String additionalPackage : additionalPackages) {
                PsiPackage psiPackage = javaPsiFacade.findPackage(additionalPackage);
                if (psiPackage == null) {
                    MainLogger.info(project, 1, "Package \"%s\" was not found.", additionalPackage);
                    continue;
                }

                MainLogger.info(project, 1, "Package \"%s\"...", psiPackage.getQualifiedName());
                for (PsiClass aClass : psiPackage.getClasses()) {
                    HandleClassesResponse response = rulesManager.handleClassAndSuperClasses(aClass);
                    MainLogger.info(project, 2, response.getMessage());
                }

            }
            MainLogger.info(project, "Rules from dependencies have been registered.");
        } catch (Exception e){
            MainLogger.warn(project, "An error occurred while registering rules from dependencies: %s, %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    public static void registerClassDeletionListener(Project project) {
        PsiManager psiManager = PsiManager.getInstance(project);
        psiManager.addPsiTreeChangeListener(new ClassDeletionListener(), () -> {});
    }

    @Override
    public void projectClosed() {
        projectPathToArtifactId.remove(project.getBasePath());
        RulesManager.getInstance(project).resetAll();
    }

    public String getArtifactId(@NotNull VirtualFile file){
        XmlFile xmlFile = Utils.parseXmlFile(project, file);
        if (xmlFile == null) return null;

        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null) return null;

        XmlTag[] subTags = rootTag.getSubTags();
        for (XmlTag subTag : subTags) {
            if ("artifactId".equals(subTag.getName())){
                return subTag.getValue().getText();
            }
        }

        return null;
    }

}

