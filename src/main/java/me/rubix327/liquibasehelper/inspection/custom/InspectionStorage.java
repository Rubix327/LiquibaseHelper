package me.rubix327.liquibasehelper.inspection.custom;

import lombok.Getter;

import java.util.List;

/**
 * Хранилище кастомных инспекций
 */
public class InspectionStorage {

    /**
     * Инспекции для XML-тегов
     */
    @Getter
    private static final List<IXmlTagInspection> xmlTagInspections = List.of(
            new ActionCodeInspection()
    );

}
