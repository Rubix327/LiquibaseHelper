package me.rubix327.liquibasehelper.locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeclinationHelper {

    CHARACTER_NOMINATIVE_TAG("tag.warn.max-length.one", "tag.warn.max-length.two", "tag.warn.max-length.five"),
    CHARACTER_NOMINATIVE_ATTR("attribute.warn.max-length.one", "attribute.warn.max-length.two", "attribute.warn.max-length.five");

    private final String one;
    private final String two;
    private final String five;

    public String getLocaleKey(long amount){
        amount = Math.abs(amount);

        if (amount >= 5 && amount <= 20) return five;

        long div = amount % 10;
        if (div == 0 || div >= 5) return five;
        if (div == 1) return one;
        return two;
    }

}
