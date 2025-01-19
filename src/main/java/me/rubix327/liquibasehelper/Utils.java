package me.rubix327.liquibasehelper;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static me.rubix327.liquibasehelper.settings.StaticSettings.ERRORS_HIGHLIGHT_TYPE;

@SuppressWarnings("unused")
public class Utils {

    public static final List<String> DATE_TIME_PATTERNS = new ArrayList<>(){{
        add("dd.MM.yyyy HH:mm:ss");
        add("dd.MM.yyyy'T'HH:mm:ss");
        add("dd.MM.yyyy HH:mm:ssXXX");
        add("dd.MM.yyyy'T'HH:mm:ssXXX");

        add("dd-MM-yyyy HH:mm:ss");
        add("dd-MM-yyyy'T'HH:mm:ss");
        add("dd-MM-yyyy HH:mm:ssXXX");
        add("dd-MM-yyyy'T'HH:mm:ssXXX");

        add("yyyy.MM.dd HH:mm:ss");
        add("yyyy.MM.dd'T'HH:mm:ss");
        add("yyyy.MM.dd HH:mm:ssXXX");
        add("yyyy.MM.dd'T'HH:mm:ssXXX");

        add("yyyy-MM-dd HH:mm:ss");
        add("yyyy-MM-dd'T'HH:mm:ss");
        add("yyyy-MM-dd HH:mm:ssXXX");
        add("yyyy-MM-dd'T'HH:mm:ssXXX");
    }};

    public static final List<String> DATE_PATTERNS = new ArrayList<>(){{
        add("dd.MM.yyyy");
        add("dd-MM-yyyy");
        add("yyyy.MM.dd");
        add("yyyy-MM-dd");
    }};

    /**
     * Открыть файл с курсором в начале
     */
    public static void openFile(@NotNull Project project, VirtualFile file) {
        if (file == null || !file.isValid()) return;
        ApplicationManager.getApplication().invokeLater(() -> FileEditorManager.getInstance(project).openFile(file, true));
    }

    /**
     * Открыть файл с курсором на указанном элементе
     */
    public static void openFile(@NotNull Project project, PsiElement element){
        openFile(project, element, element.getTextOffset());
    }

    public static void openFile(@NotNull Project project, PsiElement element, int textOffset){
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile virtualFile;
            if (element instanceof PsiFile psiFile) {
                virtualFile = psiFile.getVirtualFile();
            } else {
                try {
                    virtualFile = element.getContainingFile().getVirtualFile();
                } catch (PsiInvalidElementAccessException ignored){
                    return;
                }
            }
            new OpenFileDescriptor(project, virtualFile, textOffset).navigate(true);
        });
    }

    @NotNull
    public static String getDisplayPathCutProject(@NotNull Project project, @NotNull String path){
        String basePath = project.getBasePath();
        if (basePath == null) return path;
        int startIndex = path.indexOf(basePath);
        if (startIndex != -1){
            String cutPath = path.substring(startIndex + basePath.length());
            int sepIndex = cutPath.indexOf("/");
            if (sepIndex != -1 && cutPath.length() != 1){
                return cutPath.substring(sepIndex + 1);
            }
            return cutPath;
        }
        return path;
    }

    @Nullable
    public static XmlFile parseXmlFile(Project project, VirtualFile file) {
        try {
            PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
            return (XmlFile) fileFactory.createFileFromText(file.getName(), file.getFileType(), new String(file.contentsToByteArray()));
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static VirtualFile findFileInProject(Project project, String relativeEndPath, String... containPaths) {
        Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(
                relativeEndPath.substring(relativeEndPath.lastIndexOf("/") + 1),
                GlobalSearchScope.projectScope(project)
        );

        List<String> mustNotContainPaths = new ArrayList<>();
        List<String> mustContainPaths = new ArrayList<>();
        for (String path : containPaths) {
            if (path.startsWith("!")){
                mustNotContainPaths.add(path.replace("!", ""));
            } else {
                mustContainPaths.add(path);
            }
        }

        for (VirtualFile file : files) {
            if (isContainPaths(file.getPath(), mustContainPaths) && isNotContainPaths(file.getPath(), mustNotContainPaths) && file.getPath().endsWith(relativeEndPath)) {
                return file;
            }
        }
        return null;
    }

    @Nullable
    public static PsiFile getPsiFileFromVirtualFile(@NotNull Project project, @NotNull VirtualFile virtualFile){
        return PsiManager.getInstance(project).findFile(virtualFile);
    }

    @Nullable
    public static PsiClass findPsiClassByQualifiedName(Project project, String qualifiedName){
        if (qualifiedName == null || project == null) {
            return null;
        }

        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);

        // Ищем класс по qualifiedName
        return psiFacade.findClass(qualifiedName, GlobalSearchScope.allScope(project));
    }

    public static PsiFile findClassPsiFileByQualifiedName(Project project, String qualifiedName){
        PsiClass psiClass = findPsiClassByQualifiedName(project, qualifiedName);
        if (psiClass != null) {
            return psiClass.getContainingFile();
        }
        return null;
    }

    @Nullable
    public static PsiFile findPsiFileByQualifiedName(Project project, String qualifiedName) {
        PsiClass psiClass = findPsiClassByQualifiedName(project, qualifiedName);
        if (psiClass != null) {
            return psiClass.getContainingFile();
        }

        // Если это не класс, ищем по VirtualFile (например, для других типов файлов)
        return findNonClassFileByQualifiedName(project, qualifiedName, ".xml");
    }

    // Метод для поиска файлов, которые не являются классами
    @Nullable
    public static PsiFile findNonClassFileByQualifiedName(Project project, String qualifiedName, String extension) {
        // Преобразуем qualifiedName в относительный путь (например, com/example/MyFile.xml)
        String filePath = qualifiedName.replace('.', '/') + extension;

        // Ищем VirtualFile по пути
        VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://" + filePath);
        if (virtualFile != null) {
            // Преобразуем VirtualFile в PsiFile
            return PsiManager.getInstance(project).findFile(virtualFile);
        }

        return null;
    }

    // Метод для получения класса типа у списка
    // List<String> -> PsiClass('String')
    public static PsiClass getPsiClassFromListType(@NotNull PsiField field){
        PsiType psiType = field.getType();
        if (psiType instanceof PsiClassType classType) {

            // Проверяем, что это параметризованный тип
            PsiType[] parameters = classType.getParameters();
            if (parameters.length > 0) {
                PsiType parameterType = parameters[0];
                if (parameterType instanceof PsiClassType parameterClassType) {
                    return parameterClassType.resolve();
                }
            }
        }

        return null;
    }

    public static boolean isNotContainPaths(String path, List<String> mustNotContainPaths){
        for (String mustNotContainPath : mustNotContainPaths) {
            if (path.contains(mustNotContainPath)) return false;
        }
        return true;
    }

    public static boolean isContainPaths(String path, List<String> mustContainsPaths){
        for (String mustContainsPath : mustContainsPaths) {
            if (!path.contains(mustContainsPath)) return false;
        }
        return true;
    }

    @NotNull
    public static List<XmlTag> getXmlTagsFromPsiElements(PsiElement[] psiElements){
        List<XmlTag> xmlTags = new ArrayList<>();
        for (PsiElement psiElement : psiElements) {
            if (psiElement instanceof XmlTag tag){
                xmlTags.add(tag);
            }
        }
        return xmlTags;
    }

    public static ClassLoader getProjectTargetClassLoader(Project project){
        List<Path> paths = new ArrayList<>();
        VirtualFile[] roots = project.getBaseDir().getChildren();

        for (VirtualFile root : roots) {
            if (root.isDirectory() && "target".equals(root.getName())){
                paths.addAll(Arrays.stream(root.getChildren()).map(VirtualFile::getPath).map(Path::of).toList());
            }
        }

        return UrlClassLoader.build().files(paths).get();
    }

    public static boolean isBlank(@Nullable String string){
        return string == null || string.isBlank();
    }

    public static boolean isNotBlank(@Nullable String string){
        return string != null && !string.isBlank();
    }

    public static boolean isEmpty(@Nullable Collection<?> collection){
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(@Nullable Collection<?> collection){
        return !isEmpty(collection);
    }

    public static boolean isClassAndFileNamesNotMatch(@NotNull PsiClass psiClass){
        String containingFileName = psiClass.getContainingFile().getName();
        String classQName = psiClass.getQualifiedName();
        if (classQName == null) return true;
        if (!classQName.contains(".")) return true;

        String classShortName = classQName.substring(classQName.lastIndexOf(".") + 1);
        String fileShortName = containingFileName.replace(".java", "").replace(".class", "");

        return !classShortName.equals(fileShortName);
    }

    @NotNull
    public static String getHtmlLink(@NotNull String qualifiedName, @NotNull String text){
        return "<a href=\"psi_element://" + qualifiedName + "\">" + text + "</a>";
    }

    @Nullable
    public static String getFirstMeaningfulLineFromDocComment(@NotNull PsiField field) {
        PsiDocComment docComment = field.getDocComment();
        if (docComment == null) {
            return null; // Нет комментария
        }

        String[] lines = docComment.getText().split("\n");
        for (String line : lines) {
            // Убираем символы *, пробелы и проверяем, что строка не пустая
            String cleanedLine = line.replaceAll("\\*", "").replaceAll("/", "").trim();
            if (!cleanedLine.isEmpty()) {
                return cleanedLine; // Первая значимая строка
            }
        }
        return null; // Если не нашли значимых строк
    }

    public static void registerProblem(ProblemsHolder holder, PsiElement element, ProblemHighlightType highlightType, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), highlightType);
    }

    public static void registerProblem(ProblemsHolder holder, PsiElement element, ProblemHighlightType highlightType, LocalQuickFix quickFix, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), highlightType, quickFix);
    }

    public static void registerError(ProblemsHolder holder, PsiElement element, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), ERRORS_HIGHLIGHT_TYPE);
    }

    public static void registerError(ProblemsHolder holder, PsiElement element, LocalQuickFix fix, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), ERRORS_HIGHLIGHT_TYPE, fix);
    }

    public static void registerErrorOnElement(ProblemsHolder holder, XmlTag tag, String errorText){
        holder.registerProblem(tag, errorText, ERRORS_HIGHLIGHT_TYPE);
    }

    public static void registerErrorOnValueOrTag(ProblemsHolder holder, XmlTag tag, String errorText){
        PsiElement element = tag.getValue().getTextElements().length > 0 ? tag.getValue().getTextElements()[0] : tag;
        holder.registerProblem(element, errorText, ERRORS_HIGHLIGHT_TYPE);
    }

    public static void registerWarning(ProblemsHolder holder, PsiElement element, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), ProblemHighlightType.WARNING);
    }

    public static void registerWarning(ProblemsHolder holder, PsiElement element, LocalQuickFix quickFix, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), ProblemHighlightType.WARNING, quickFix);
    }

    public static boolean isDate(String s){
        return getDate(s) != null;
    }

    public static boolean isClassOfAnyType(@NotNull PsiClass psiClass, @NotNull Class<?>... types){
        return Arrays.stream(types).map(Class::getName).anyMatch(t -> t.equals(psiClass.getQualifiedName()));
    }

    public static LocalDateTime getDate(String s){
        for (String dateFormat : DATE_TIME_PATTERNS) {
            try{
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

                return LocalDateTime.parse(s, formatter);
            } catch (DateTimeParseException ignored){}
        }
        for (String dateFormat : DATE_PATTERNS){
            try{
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
                LocalDate ld = LocalDate.parse(s, formatter);
                return LocalDateTime.of(ld, LocalTime.of(0, 0, 0));
            } catch (DateTimeParseException ignored){}
        }
        return null;
    }

    public static void runReadActionInBackground(@NotNull Project project, @NotNull String title, Runnable runnable){
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                ApplicationManager.getApplication().runReadAction(runnable);
            }
        });
    }

    public static void optimizeImports(@NotNull Project project, @NotNull PsiFile file) {
        if (file instanceof PsiJavaFile) {
            JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
            styleManager.optimizeImports(file);
        }
    }

}
