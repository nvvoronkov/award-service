package org.example.service.parser;

import org.example.model.dto.AwardUploadRow;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CsvAwardFileParser {

    public Flux<AwardUploadRow> parse(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        AtomicInteger rowCounter = new AtomicInteger(0);

        return Flux.fromStream(reader.lines())
                .skip(1)
                .map(line -> {
                    int rowNum = rowCounter.incrementAndGet() + 1;
                    String[] parts = line.split(",", -1);

                    if (parts.length < 5) {
                        throw new IllegalArgumentException("Not enough columns in a row " + rowNum);
                    }

                    AwardUploadRow row = new AwardUploadRow();
                    row.setRowNumber(rowNum);
                    row.setEmployeeId(Long.parseLong(parts[0].trim()));
                    row.setEmployeeFullName(parts[1].trim());
                    row.setAwardCode(parts[2].trim());
                    row.setAwardName(parts[3].trim());
                    row.setAwardDate(LocalDate.parse(parts[4].trim()));
                    return row;
                });
    }
}
