package ru.nikitavov.service;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import ru.nikitavov.data.*;
import ru.nikitavov.exception.ProcessingException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataConvertService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void processing(String link, String login, String pass) {
        URI uri = URI.create(link);
        String base64Credentials = Base64.getEncoder().encodeToString((login + ":" + pass).getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + base64Credentials;

        String data = sendGetData(uri, authHeader);
        Map<Integer, NewDepartment> convertedData = convertData(data);
        String convertedDataString = convertConvertedDataToString(convertedData);
        sendPostData(uri, authHeader, convertedDataString);
        saveConvertedDataToFile(convertedData);
    }

    private static String sendGetData(URI uri, String authHeader) {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Authorization", authHeader).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            validateResponseCode(response);

            System.out.println("Data was successfully received from the server!");
            return response.body();

        } catch (IOException | InterruptedException e) {
            throw new ProcessingException("An error occurred while sending a GET request to %s!".formatted(uri.toString()));
        }
    }

    private static void validateResponseCode(HttpResponse<String> response) {
        if (response.statusCode() == 401) {
            throw new ProcessingException("Authorization data is not valid!");
        }

        if (response.statusCode() == 404) {
            throw new ProcessingException("No data found!");
        }

        if (response.statusCode() == 500) {
            throw new ProcessingException("An error occurred on the server side!");
        }

        if (!Integer.toString(response.statusCode()).startsWith("2")) {
            throw new ProcessingException("The server returned code %s, although 2** was expected!".formatted(response.statusCode()));
        }
    }

    private static Map<Integer, NewDepartment> convertData(String responseBody) {
        try {
            OldObject oldObject = objectMapper.readValue(responseBody, OldObject.class);

            Map<OldDepartment, List<OldEmployer>> groupedData = oldObject.employees().stream()
                    .collect(Collectors.groupingBy(OldEmployer::department));

            return groupedData.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().id(),
                            entry -> createNewDepartment(entry.getKey(), entry.getValue())
                    ));

        } catch (Exception e) {
            throw new ProcessingException("An error occurred while working with Json!");
        }
    }

    private static String convertConvertedDataToString(Map<Integer, NewDepartment> convertedData) {
        try {
            return objectMapper.writeValueAsString(convertedData);
        } catch (Exception e) {
            throw new ProcessingException("An error occurred while working with Json!");
        }
    }

    private static NewDepartment createNewDepartment(OldDepartment oldDepartment, List<OldEmployer> oldEmployers) {
        return new NewDepartment(oldDepartment.departmentName(), createNewEmployers(oldEmployers));
    }

    private static List<NewEmployer> createNewEmployers(List<OldEmployer> oldEmployers) {
        return oldEmployers.stream().map(oldEmployer -> new NewEmployer(oldEmployer.name(), oldEmployer.age())).toList();
    }

    private static void sendPostData(URI uri, String authHeader, String convertedData) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(convertedData))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            validateResponseCode(response);

            System.out.println("Data sent to server successfully!");

        } catch (IOException | InterruptedException e) {
            throw new ProcessingException("An error occurred when sending a POST request to %s, with the body \"%s\"!".formatted(uri.toString(), convertedData));
        }
    }

    private static void saveConvertedDataToFile(Map<Integer, NewDepartment> convertedData) {
        String fileName = new Date().toString().replace(":", "-") + ".json";
        try {
            File file = new File("result");
            if (!file.exists()) {
                file.mkdir();
            }
            ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter());

                writer.writeValue(new File("result/" + fileName), convertedData);

            System.out.printf("Data was successfully written to file %s!%n", fileName);
        } catch (IOException e) {
            throw new ProcessingException("An error occurred while saving the converted data to a file!");
        }
    }
}
