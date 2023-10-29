package ru.nikitavov.service;

import ru.nikitavov.exception.ProcessingException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ParametersService {

    public static String LINK = "link";
    public static String LOGIN = "login";
    public static String PASSWORD = "pass";

    public static Map<String, String> parse(String[] args) {
        Map<String, String> result = new HashMap<>();
        String paramKey = null;

        for (String arg : args) {
            if (arg.startsWith("--")) {
                paramKey = arg.substring(2);
                continue;
            }

            if (paramKey == null) {
                continue;
            }

            result.put(paramKey, arg);
            paramKey = null;
        }

        return result;
    }

    public static void validate(Map<String, String> params) {
        validateParam(params, LOGIN);
        validateParam(params, PASSWORD);
        validateParam(params, LINK);
        validateUrl(params, LINK);
    }

    private static void validateParam(Map<String, String> params, String paramName) {
        if (!params.containsKey(paramName)) {
            throw new ProcessingException("Parameter %s not specified!".formatted(paramName));
        }
    }

    private static void validateUrl(Map<String, String> params, String paramName) {
        try {
            new URI(params.get(paramName));
        } catch (URISyntaxException e) {
            throw new ProcessingException("Parameter %s is not a URL!".formatted(paramName));
        }
    }
}
