package me.rubix327.liquibasehelper.inspection.model;

import com.intellij.psi.PsiClass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class DatamodelClassCheckResponse {

    private final @Nullable PsiClass baseClass;
    private final boolean isDatamodelClass;
    private String message;

}
