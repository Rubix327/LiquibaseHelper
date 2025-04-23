package me.rubix327.liquibasehelper;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.extensions.PluginId;
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
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.*;
import java.util.stream.Collectors;

import static me.rubix327.liquibasehelper.inspection.model.HandleClassesResponse.ErrorReason.*;

public class StartProjectComponent implements ProjectComponent, Disposable {

    private static final List<String> projectsRegisteredToUpdateRules = new ArrayList<>();
    private static final Map<String, String> projectPathToArtifactId = new HashMap<>();
    private final Project project;

    private static final int SKIPPED_CLASSES_LOG_LEVEL = 0; // debug variable

    public StartProjectComponent(Project project) {
        this.project = project;
    }

    /**
     * Получить название артефакта указанного проекта (берется из атрибута artifactId в pom.xml).
     * @param project Проект
     * @return Название артефакта (проекта)
     */
    public static String getArtifactId(Project project){
        return projectPathToArtifactId.get(project.getBasePath());
    }

    @Override
    public void projectOpened() {
        MainLogger.info(project, "Loading settings:");
        for (String s : PersistentUserSettings.getInstance().toString().split("\n")) {
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

        registerRulesForAllClassesAfterIndexingInBackground(project);
        registerMavenReloadListener();
    }

    /**
     * Зарегистрировать все правила указанного проекта.<br>
     * Если регистрация уже запланирована и еще не закончена, то новая запланирована не будет.<br>
     * Непосредственно регистрация правил запускается только тогда, когда проект входит в режим "умной" работы
     * (или сразу, если проект уже находится в этом режиме).
     * @param project Проект
     */
    public static void registerRulesForAllClassesAfterIndexingInBackground(@NotNull Project project){
        // Предотвращение запуска регистрации правил несколько раз, пока не завершится хотя бы одна регистрация.
        // Например, при смене git-веток во время индексации, могут удаляться сразу несколько файлов (N),
        // и раньше это приводило к отложенному запуску регистрации правил N раз.
        if (projectsRegisteredToUpdateRules.contains(project.getBasePath())){
            return;
        }
        projectsRegisteredToUpdateRules.add(project.getBasePath());

        // Проверяем, что проект уже в режиме "умной" работы (индексация завершена)
        if (!DumbService.getInstance(project).isDumb()) {
            MainLogger.info(project, "The project is in smart mode.");
            registerRulesForAllClassesInBackground(project);
        } else {
            // Если индексация еще не завершена, регистрируем слушателя
            MainLogger.info(project, "The project is in dumb mode. Rules will be registered later.");
            DumbService.getInstance(project).runWhenSmart(() -> registerRulesForAllClassesInBackground(project));
        }
    }

    private static void registerRulesForAllClassesInBackground(@NotNull Project project){
        Utils.runReadActionInBackground(project, "LiquibaseHelper: Loading rules", () -> registerRulesForAllClasses(project));
    }

    // Зарегистрировать все правила проекта
    private static void registerRulesForAllClasses(Project project){
        if (project.isDisposed()){
            MainLogger.warn("Called project is already disposed: %s", project.getName());
            return;
        }

        try {
            RulesManager rulesManagerInstance = RulesManager.getInstance(project);
            rulesManagerInstance.resetAll();
            Query<PsiClass> allClasses = AllClassesSearch.search(GlobalSearchScope.projectScope(project), project);

            MainLogger.info(project, "Registering project-level rules...");
            boolean atLeastOneRegistered = false;
            List<HandleClassesResponse> skippedResponses = new ArrayList<>();
            for (PsiClass psiClass : allClasses) {
                HandleClassesResponse response = rulesManagerInstance.handleClassAndSuperClasses(psiClass, "StartProjectComponent: project");
                if (response.isSuccess()){
                    MainLogger.info(project, 1, response.getMessage());
                    atLeastOneRegistered = true;
                } else if (SKIPPED_CLASSES_LOG_LEVEL == 2) {
                    MainLogger.info(project, 1, "Skipped class %s: %s", psiClass.getQualifiedName(), response.getMessage());
                    skippedResponses.add(response);
                }
            }

            if (SKIPPED_CLASSES_LOG_LEVEL == 1){
                logSkippedClasses(project, skippedResponses, 1);
            }

            if (atLeastOneRegistered){
                MainLogger.info(project, "Project-level rules have been registered.");
            } else {
                MainLogger.info(project, "No project-level rules have been registered.");
            }

            registerRulesFromDependencies(rulesManagerInstance);
            rulesManagerInstance.printAllRules();
        } finally {
            projectsRegisteredToUpdateRules.remove(project.getBasePath());
        }
    }

    // Зарегистрировать правила из зависимостей Maven
    private static void registerRulesFromDependencies(RulesManager rulesManager){
        Project project = rulesManager.getProject();
        if (project.isDisposed()){
            MainLogger.warn("Called project is already disposed: %s", project.getName());
            return;
        }

        try {
            MainLogger.info(project, "Registering rules from dependencies...");
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);

            // TODO вынести в настройки
            Map<String, List<String>> modulesToAdditionalPackages = Map.of(
                    "cbscoreservices-metaloader", List.of(
                            "ru.athena.cbs.base.metaloader.metaentity",
                            "ru.athena.cbs.base.metaloader.metaentity.addtitionalattributes",
                            "ru.athena.cbs.coreservices.metaloader.entitylabels.metaentity",
                            "ru.athena.cbs.coreservices.metaloader.enumeration.metaentity",
                            "ru.athena.cbs.coreservices.metaloader.externaldocid.metaentity",
                            "ru.athena.cbs.coreservices.metaloader.metaentity",
                            "ru.athena.cbs.coreservices.metaloader.registrykey.metaentity",
                            "ru.athena.cbs.coreservices.metaloader.userkey.metaentity"
                    ),
                    "cbsdocengine-metaloader", List.of(
                            "ru.athena.cbs.docengine.metaloader.metaentity"
                    ),
                    "cbsdocnumber-metaloader", List.of(
                            "ru.athena.cbs.cbsdocnumber.metaloader.metaentity"
                    )
            );

            for (Map.Entry<String, List<String>> moduleToAdditionalPackage : modulesToAdditionalPackages.entrySet()) {
                for (String pack : moduleToAdditionalPackage.getValue()) {
                    PsiPackage psiPackage = javaPsiFacade.findPackage(pack);
                    if (psiPackage == null){
                        MainLogger.info(project, 1, "Package \"%s\" was not found.", pack);
                        continue;
                    }

                    MainLogger.info(project, 1, "Package \"%s\"...", psiPackage.getQualifiedName());
                    PsiClass[] classes = psiPackage.getClasses();
                    if (classes.length == 0){
                        MainLogger.info(project, 2, "No classes found.");
                    }

                    for (PsiClass aClass : classes) {
                        if (aClass.getContainingFile() == null) continue;
                        if (aClass.getContainingFile().getVirtualFile() == null) continue;
                        // Проверка, что это класс именно из .jar нужного модуля
                        // Раньше плагин находил эти классы во всех зависимостях проекта (н-р, auth, currency), и возникали расхождения правил
                        if (!aClass.getContainingFile().getVirtualFile().getPath().contains("/" + moduleToAdditionalPackage.getKey() + "/")) continue;

                        HandleClassesResponse response = rulesManager.handleClassAndSuperClasses(aClass, "StartProjectComponent: dependencies");
                        if (response.isSuccess()){
                            MainLogger.info(project, 2, response.getMessage());
                        }
                    }
                }
            }

            MainLogger.info(project, "Rules from dependencies have been registered.");

        } catch (Exception e){
            MainLogger.warn(project, "An error occurred while registering rules from dependencies: %s, %s", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    // Зарегистрировать слушатель перезагрузки Maven
    private void registerMavenReloadListener(){
        ApplicationManager.getApplication().invokeLater(() -> {
            if (PluginManagerCore.isPluginInstalled(PluginId.getId("org.jetbrains.idea.maven"))){
                MavenProjectsManager mavenManager = MavenProjectsManager.getInstance(project);
                if (mavenManager != null && mavenManager.isMavenizedProject()) {
                    MainLogger.info(project, "Registering MavenReloadListener...");
                    mavenManager.addManagerListener(new MavenProjectsManager.Listener() {
                        @Override
                        public void projectImportCompleted() {
                            MainLogger.info(project, "Maven reloaded: updating project-level rules...");
                            StartProjectComponent.registerRulesForAllClassesAfterIndexingInBackground(project);
                        }
                    }, this);
                }
            }
        });
    }

    private static void logSkippedClasses(Project project, List<HandleClassesResponse> skippedResponses, int baseOffset){
        if (!skippedResponses.isEmpty()){
            MainLogger.info(project, baseOffset, "Skipped classes (%s):", skippedResponses.size());
        }
        logSkippedClasses(project, skippedResponses, baseOffset + 1, CLASS_IS_NOT_DATAMODEL, "- Not datamodel classes: {count}");
        logSkippedClasses(project, skippedResponses, baseOffset + 1, CLASS_IS_MAPPED, "- Mapped classes: {count}");
        logSkippedClasses(project, skippedResponses, baseOffset + 1, CLASS_IS_INNER, "- Inner classes: {count}");
        logSkippedClasses(project, skippedResponses, baseOffset + 1, CLASS_IS_ENUM, "- Enum classes: {count}");
        logSkippedClasses(project, skippedResponses, baseOffset + 1, CANNOT_GET_QUALIFIED_NAME, "- Could not get qualified name: {count}");
        logSkippedClasses(project, skippedResponses, baseOffset + 1, CANNOT_GET_DATAMODEL_TAG, "- Could not get datamodel tag: {count}");
    }

    private static void logSkippedClasses(Project project, List<HandleClassesResponse> skippedResponses, int offset,
                                          HandleClassesResponse.ErrorReason errorReason, String msg){
        Set<String> skipped = skippedResponses.stream()
                .filter(r -> r.getErrorReason() == errorReason)
                .map(r -> r.getBaseClass().getName())
                .collect(Collectors.toSet());
        if (Utils.isNotEmpty(skipped)){
            MainLogger.info(project, offset, msg
                    .replace("{count}", String.valueOf(skipped.size()))
                    .replace("{classes}", String.valueOf(skipped))
            );
        }
    }

    private static void registerClassDeletionListener(Project project) {
        PsiManager psiManager = PsiManager.getInstance(project);
        psiManager.addPsiTreeChangeListener(new ClassDeletionListener(), () -> {});
    }

    @Override
    public void projectClosed() {
        dispose();
    }

    private String getArtifactId(@NotNull VirtualFile file){
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

    @Override
    public void dispose() {
        projectPathToArtifactId.remove(project.getBasePath());
        projectsRegisteredToUpdateRules.remove(project.getBasePath());
        RulesManager.removeInstance(project);
    }

}

