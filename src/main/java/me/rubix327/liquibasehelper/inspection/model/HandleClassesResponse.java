package me.rubix327.liquibasehelper.inspection.model;

import com.intellij.psi.PsiClass;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class HandleClassesResponse {

    private final @NotNull PsiClass baseClass;
    private boolean success;
    private String message;
    private ErrorReason errorReason;

    public HandleClassesResponse(@NotNull PsiClass baseClass) {
        this.baseClass = baseClass;
    }

    public HandleClassesResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public HandleClassesResponse setMessage(@NotNull String message, Object... args) {
        this.message = String.format(message, args).replace("{class}", baseClass.getName() == null ? "null" : baseClass.getName());
        return this;
    }

    public HandleClassesResponse setErrorReason(ErrorReason reason){
        this.errorReason = reason;
        return this;
    }

    public static HandleClassesResponse makeErrorResponse(@NotNull PsiClass psiClass, @NotNull ErrorReason errorReason){
        return new HandleClassesResponse(psiClass).setSuccess(false).setMessage(errorReason.getMessage()).setErrorReason(errorReason);
    }

    @Getter
    @RequiredArgsConstructor
    public enum ErrorReason{
        CLASS_IS_NOT_DATAMODEL("Skipping class {class} (not a datamodel class)."),
        CLASS_IS_MAPPED("Skipping class {class} (the class is mapped)."),
        CLASS_IS_INNER("Skipping class {class} (the class is inner)."),
        CLASS_IS_ENUM("Skipping class {class} (the class is enum)."),
        CANNOT_GET_QUALIFIED_NAME("Skipping class {class} (could not get qualified name of class)."),
        CANNOT_GET_DATAMODEL_TAG("Skipping class {class} (could not get datamodel tag of class).");

        private final String message;
    }

}
