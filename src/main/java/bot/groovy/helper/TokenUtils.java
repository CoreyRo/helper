package bot.groovy.helper;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class TokenUtils {

    public static String getUserId(String token) {
        return getDecodedTokenParts(token).get(0);
    }

    public static List<String> getDecodedTokenParts(String token) {
        return Arrays.stream(token.split("\\."))
            .map(Base64.getDecoder()::decode)
            .map(String::new)
            .collect(Collectors.toList());
    }

}
