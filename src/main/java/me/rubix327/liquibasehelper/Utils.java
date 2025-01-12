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
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.lang.UrlClassLoader;
import me.rubix327.liquibasehelper.settings.CbsAnnotation;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static me.rubix327.liquibasehelper.settings.StaticSettings.ERRORS_HIGHLIGHT_TYPE;

@SuppressWarnings("unused")
public class Utils {

    public static final List<String> DATE_FORMATS = new ArrayList<>(){{
        add("dd.MM.yyyy");
        add("dd.MM.yyyy HH:mm:ss");
        add("dd.MM.yyyy'T'HH:mm:ss");
        add("dd.MM.yyyy HH:mm:ssXXX");
        add("dd.MM.yyyy'T'HH:mm:ssXXX");

        add("dd-MM-yyyy");
        add("dd-MM-yyyy HH:mm:ss");
        add("dd-MM-yyyy'T'HH:mm:ss");
        add("dd-MM-yyyy HH:mm:ssXXX");
        add("dd-MM-yyyy'T'HH:mm:ssXXX");

        add("yyyy.MM.dd");
        add("yyyy.MM.dd HH:mm:ss");
        add("yyyy.MM.dd'T'HH:mm:ss");
        add("yyyy.MM.dd HH:mm:ssXXX");
        add("yyyy.MM.dd'T'HH:mm:ssXXX");

        add("yyyy-MM-dd");
        add("yyyy-MM-dd HH:mm:ss");
        add("yyyy-MM-dd'T'HH:mm:ss");
        add("yyyy-MM-dd HH:mm:ssXXX");
        add("yyyy-MM-dd'T'HH:mm:ssXXX");
    }};

    /**
     * Открыть файл с курсором в начале
     */
    public static void openFile(Project project, VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() -> FileEditorManager.getInstance(project).openFile(file, true));
    }

    /**
     * Открыть файл с курсором на указанном элементе
     */
    public static void openFile(Project project, PsiElement element){
        openFile(project, element, element.getTextOffset());
    }

    public static void openFile(Project project, PsiElement element, int textOffset){
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile virtualFile;
            if (element instanceof PsiFile psiFile) {
                virtualFile = psiFile.getVirtualFile();
            } else {
                virtualFile = element.getContainingFile().getVirtualFile();
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

    /**
     * Найти аннотацию над объектом.<br><br>
     * Если на вход подается полный путь аннотации, то она ищется по полному совпадению.<br>
     * Если на вход подается только название аннотации, то она ищется по вхождению в QuilifiedName каждой аннотации над полем.
     * @param annotationHolder Объект, над которым может висеть аннотация
     * @param annotationName Название аннотации или полный путь до нее (qualifiedName)
     * @return Аннотация PsiAnnotation
     */
    @Deprecated(forRemoval = true)
    public static PsiAnnotation findAnnotation(@NotNull PsiJvmModifiersOwner annotationHolder, @NotNull String annotationName){
        if (annotationName.contains(".")){
            return annotationHolder.getAnnotation(annotationName);
        }

        for (PsiAnnotation annotation : annotationHolder.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) continue;

            if (qualifiedName.contains(annotationName)){
                return annotation;
            }
        }
        return null;
    }

    @Nullable
    public static PsiAnnotation findAnnotation(@NotNull PsiJvmModifiersOwner annotationHolder, @NotNull CbsAnnotation cbsAnnotation){
        if (StaticSettings.SEARCH_ANNOTATION_BY_QUALIFIED_NAME){
            return annotationHolder.getAnnotation(cbsAnnotation.getQualifiedName());
        }

        for (PsiAnnotation annotation : annotationHolder.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) continue;

            if (qualifiedName.contains(cbsAnnotation.getShortName())){
                return annotation;
            }
        }
        return null;
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

    private static boolean isClassInvalid(@Nullable PsiClass psiClass){
        if (psiClass == null) return true;
        if (Object.class.getName().equals(psiClass.getQualifiedName())) return true;
        if (psiClass.getQualifiedName() == null || !psiClass.getQualifiedName().contains(".metaloader.")) return true;
        return psiClass.getContainingClass() != null;
    }

    /**
     * Проверить, что класс не является CbsDatamodelClass.<br>
     * Если этот метод возвращает true, то это гарантирует, что как минимум одно из следующих условий верно:<ul>
     * <li>Указанный psiClass == null</li>
     * <li>У указанного psiClass нет qualifiedName</li>
     * <li>Указанный psiClass не находится в пакете metaloader</li>
     * <li>Указанный psiClass вложен в другие классы</li>
     * <li>Указанный psiClass является перечислением (enum)</li>
     * <li>Над указанным psiClass нет аннотации @CbsDatamodelClass</li>
     * </ul>
     */
    public static boolean isNotDatamodelClass(@Nullable PsiClass psiClass){
        if (isClassInvalid(psiClass)) return true;
        if (psiClass.isEnum()) return true;
        return Utils.findAnnotation(psiClass, CbsAnnotation.CbsDatamodelClass.INSTANCE) == null;
    }

    public static boolean isDatamodelMappedClass(@Nullable PsiClass psiClass){
        if (isClassInvalid(psiClass)) return false;
        PsiAnnotation annotation = Utils.findAnnotation(psiClass, CbsAnnotation.CbsDatamodelClass.INSTANCE);
        if (annotation == null) return false;
        PsiAnnotationMemberValue isMappedMember = annotation.findDeclaredAttributeValue(CbsAnnotation.CbsDatamodelClass.Fields.MAPPED);
        if (isMappedMember instanceof PsiLiteralExpression isMappedBoolean){
            if (isMappedBoolean.getValue() instanceof Boolean isMappedValue){
                return isMappedValue;
            }
        }
        return false;
    }

    @Nullable
    public static String getCbsDatamodelClassAnnotationFieldValue(@NotNull PsiClass psiClass, String field){
        if (isNotDatamodelClass(psiClass)) return null;
        return getAnnotationFieldValue(psiClass, CbsAnnotation.CbsDatamodelClass.INSTANCE, field);
    }

    @Nullable
    public static String getAnnotationFieldValue(@NotNull PsiClass psiClass, CbsAnnotation annotation, String fieldName){
        PsiAnnotation psiAnnotation = Utils.findAnnotation(psiClass, annotation);
        if (psiAnnotation == null){
            return null;
        }

        PsiAnnotationMemberValue tooltip = psiAnnotation.findDeclaredAttributeValue(fieldName);
        if (tooltip instanceof PsiLiteralExpression tooltipString){
            if (tooltipString.getValue() instanceof String fieldValue && Utils.isNotBlank(fieldValue)){
                return fieldValue;
            }
        }
        return null;
    }

    public static boolean isClassAndFileNamesNotMatch(@NotNull PsiClass psiClass){
        String containingFileName = psiClass.getContainingFile().getName();
        String classQName = psiClass.getQualifiedName();
        if (classQName == null) return true;
        if (!classQName.contains(".")) return true;

        return !classQName.substring(classQName.lastIndexOf(".") + 1).equals(containingFileName
                .replace(".java", "")
                .replace(".class", "")
        );
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

    public static void registerError(ProblemsHolder holder, PsiElement element, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), ERRORS_HIGHLIGHT_TYPE);
    }

    public static void registerError(ProblemsHolder holder, ProblemHighlightType highlightType, PsiElement element, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), highlightType);
    }

    public static void registerError(ProblemsHolder holder, ProblemHighlightType highlightType, LocalQuickFix quickFix, PsiElement element, String errorText, Object... args){
        holder.registerProblem(element, String.format(errorText, args), highlightType, quickFix);
    }

    public static void registerErrorOnElement(ProblemsHolder holder, XmlTag tag, String errorText){
        holder.registerProblem(tag, errorText, ERRORS_HIGHLIGHT_TYPE);
    }

    public static void registerErrorOnValueOrTag(ProblemsHolder holder, XmlTag tag, String errorText){
        PsiElement element = tag.getValue().getTextElements().length > 0 ? tag.getValue().getTextElements()[0] : tag;
        holder.registerProblem(element, errorText, ERRORS_HIGHLIGHT_TYPE);
    }

    public static boolean isDate(String s){
        for (String dateFormat : DATE_FORMATS) {
            try{
                if (new SimpleDateFormat(dateFormat).parse(s) != null) {
                    return true;
                }
            } catch (ParseException ignored){}
        }
        return false;
    }

    public static void runReadActionInBackground(@NotNull Project project, @NotNull String title, Runnable runnable){
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                ApplicationManager.getApplication().runReadAction(runnable);
            }
        });
    }

}
