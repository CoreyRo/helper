package bot.groovy.helper.util;

public class MiscUtils {

    private static final String[] NUMBER_NAMES = {
        "zero",
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine"
    };

    public static String getNumberName(int number) {
        if(number < 0 || number > 9) {
            throw new IllegalArgumentException("Number must be between 0 and 9 (inclusive)");
        }

        return NUMBER_NAMES[number];
    }

}
