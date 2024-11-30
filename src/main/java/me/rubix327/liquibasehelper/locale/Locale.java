package me.rubix327.liquibasehelper.locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Locale {

    EN("en"),
    RU("ru");

    private final String name;

}
