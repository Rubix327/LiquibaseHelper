package me.rubix327.liquibasehelper.inspection.model;

import com.intellij.psi.PsiClass;
import lombok.Getter;
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

    public HandleClassesResponse setMessage(String message, Object... args) {
        this.message = String.format(message, args).replace("{class}", baseClass.getName() == null ? "null" : baseClass.getName());
        return this;
    }

    public HandleClassesResponse setErrorReason(ErrorReason reason){
        this.errorReason = reason;
        return this;
    }

    public static HandleClassesResponse makeErrorResponse(PsiClass psiClass, ErrorReason errorReason){
        return new HandleClassesResponse(psiClass).setSuccess(false).setMessage(errorReason.name()).setErrorReason(errorReason);
    }

    public static HandleClassesResponse makeErrorResponse(PsiClass psiClass, ErrorReason errorReason, String message, Object... args){
        return new HandleClassesResponse(psiClass).setSuccess(false).setMessage(message, args).setErrorReason(errorReason);
    }

    public enum ErrorReason{
        CLASS_IS_NOT_DATAMODEL,
        CLASS_IS_MAPPED,
        CLASS_IS_INNER,
        CANNOT_GET_QUALIFIED_NAME,
        CANNOT_GET_DATAMODEL_TAG
    }

}
