package me.rubix327.liquibasehelper.inspection.model;

import com.intellij.psi.PsiClass;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
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

    public static HandleClassesResponse makeErrorResponse(@NotNull PsiClass psiClass, @NotNull ErrorReason errorReason, Object... args){
        return new HandleClassesResponse(psiClass).setSuccess(false).setMessage(errorReason.getMessage(args)).setErrorReason(errorReason);
    }

    @Getter
    @RequiredArgsConstructor
    public enum ErrorReason{
        CLASS_IS_NOT_DATAMODEL("Not a datamodel class (%s)"),
        CLASS_IS_MAPPED("The class is mapped"),
        CLASS_IS_INNER("The class is inner"),
        CLASS_IS_ENUM("The class is enum"),
        CANNOT_GET_QUALIFIED_NAME("Could not get qualified name of class"),
        CANNOT_GET_DATAMODEL_TAG("Could not get datamodel tag of class");

        private final String message;

        public String getMessage(Object... args){
            int occurrences = StringUtils.countMatches(message, "%s");
            String msg;
            if (args == null || args.length == 0 || args.length != occurrences){
                msg = message.replace("(%s)", "(~)").replace("%s", "~");
            } else {
                msg = String.format(message, args);
            }
            return msg;
        }
    }

}
