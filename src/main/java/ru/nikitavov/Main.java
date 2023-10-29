package ru.nikitavov;

import ru.nikitavov.exception.ProcessingException;
import ru.nikitavov.service.DataConvertService;
import ru.nikitavov.service.ParametersService;

import java.util.Map;

public class Main {

    public static void main(String[] args) {
        try {
            Map<String, String> params = ParametersService.parse(args);
            ParametersService.validate(params);

            DataConvertService.processing(params.get(ParametersService.LINK), params.get(ParametersService.LOGIN), params.get(ParametersService.PASSWORD));
        } catch (ProcessingException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

}