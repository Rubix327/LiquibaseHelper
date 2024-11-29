package me.rubix327.liquibasehelper.backreference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.log.MainLogger;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class UsageFinderUtil {

    public static int findUsagesCount(Project project, VirtualFile fileA) {
        PsiFile psiFileA = PsiManager.getInstance(project).findFile(fileA);
        if (psiFileA == null) return 0;

        // Ищем все ссылки на файл A в пределах проекта
        return ReferencesSearch.search(psiFileA, GlobalSearchScope.projectScope(project)).findAll().size();
    }

    public static List<VirtualFile> findUsagesVirtualFiles(Project project, VirtualFile fileA) {
        PsiFile psiFileA = PsiManager.getInstance(project).findFile(fileA);
        List<VirtualFile> usages = new ArrayList<>();

        if (psiFileA != null) {
            // Ищем ссылки на файл A и добавляем их в список
            ReferencesSearch.search(psiFileA, GlobalSearchScope.projectScope(project)).forEach(reference -> {
                usages.add(reference.getElement().getContainingFile().getVirtualFile());
                return true;
            });
        }
        return usages;
    }

    public static List<PsiElement> findUsages(Project project, VirtualFile findUsagesOfFile) {
        PsiFile psiFileA = PsiManager.getInstance(project).findFile(findUsagesOfFile);
        List<PsiElement> usages = new ArrayList<>();

        if (psiFileA != null) {
            // Ищем ссылки на файл A и добавляем их в список
            ReferencesSearch.search(psiFileA, GlobalSearchScope.projectScope(project)).forEach(reference -> {
                String referenceName = reference.getElement().getContainingFile() == null ? String.valueOf(reference.getElement()) :
                        reference.getElement().getContainingFile().getVirtualFile() == null ? reference.getElement().getContainingFile().getName() :
                                Utils.getDisplayPathCutProject(project, reference.getElement().getContainingFile().getVirtualFile().getPath());

                String logString = String.format("Back reference of \"%s\" is \"%s\" —— ",
                        Utils.getDisplayPathCutProject(project, psiFileA.getVirtualFile().getPath()),
                        referenceName);

                if (reference.getElement().getContainingFile() == null) {
                    MainLogger.info(project, logString + "skipping (element has no containing file).");
                    return;
                }
                VirtualFile virtualFile = reference.getElement().getContainingFile().getVirtualFile();
                if (virtualFile == null) {
                    MainLogger.info(project, logString + "skipping (could not find virtual file from element's containing file).");
                    return;
                }
                if (virtualFile.getPath().contains("/target/")) {
                    MainLogger.info(project, logString + "skipping (virtual file path contains \"/target/\").");
                    return;
                }

                MainLogger.info(project, logString + "added to usages.");
                usages.add(reference.getElement());
            });
        }
        return usages;
    }

}
