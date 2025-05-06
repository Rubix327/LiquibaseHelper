package me.rubix327.liquibasehelper.inspection.custom;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.xml.XmlTag;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.locale.Localization;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

public class ActionCodeInspection implements AbstractXmlTagInspection {

    public void checkForTagConstraints(@NotNull XmlTag tag, @NotNull ProblemsHolder holder){
        if (!"entityOperationMeta".equals(tag.getName())){
            return;
        }

        XmlTag[] subTags = tag.getSubTags();
        XmlTag actionTypeCodeTag = Utils.getXmlTagByName(subTags, "actionTypeCode", true);
        XmlTag actionCodeTag = Utils.getXmlTagByName(subTags, "actionCode", true);

        // Если <actionTypeCode> не заполнен, то ничего проверять не надо
        if (actionTypeCodeTag == null || StringUtils.isBlank(actionTypeCodeTag.getValue().getText())){
            return;
        }

        String actionTypeCodeValue = actionTypeCodeTag.getValue().getText();

        if ("ExecuteService".equals(actionTypeCodeValue)){
            // actionCode должен быть пустым или в виде пути до метода бина
            if (actionCodeTag != null && StringUtils.isNotBlank(actionCodeTag.getValue().getText())){
                String regex = "(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)+[a-zA-Z_][a-zA-Z0-9_]*";
                if (!Pattern.matches(regex, actionCodeTag.getValue().getText())){
                    Utils.registerError(holder, actionCodeTag, Localization.message("tag.warn.custom.action-code.execute-service"));
                }
            }
        }
        else if (List.of("StartBusinessProcess", "StartSTPProcess", "SendNotification").contains(actionTypeCodeValue)){
            // actionCode должен быть заполнен
            if (actionCodeTag == null){
                Utils.registerError(holder, actionTypeCodeTag, Localization.message("tag.warn.custom.action-code.start-business-process.null", actionTypeCodeValue));
            } else if (StringUtils.isBlank(actionCodeTag.getValue().getText())){
                Utils.registerError(holder, actionCodeTag, Localization.message("tag.warn.custom.action-code.start-business.process.empty", actionTypeCodeValue));
            }
        }

    }

}
