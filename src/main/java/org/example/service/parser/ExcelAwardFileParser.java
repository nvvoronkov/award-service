package org.example.service.parser;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.dto.AwardUploadRow;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelAwardFileParser {

    public Flux<AwardUploadRow> parse(InputStream inputStream) {
        return Flux.using(
                () -> new XSSFWorkbook(inputStream),
                workbook -> {
                    Sheet sheet = workbook.getSheetAt(0);
                    List<AwardUploadRow> rows = new ArrayList<>();

                    int firstRow = 1;
                    for (int i = firstRow; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) {
                            continue;
                        }

                        AwardUploadRow uploadRow = new AwardUploadRow();
                        uploadRow.setRowNumber(i + 1);

                        uploadRow.setEmployeeId((long) row.getCell(0).getNumericCellValue());
                        uploadRow.setEmployeeFullName(row.getCell(1).getStringCellValue());
                        uploadRow.setAwardCode(row.getCell(2).getStringCellValue());
                        uploadRow.setAwardName(row.getCell(3).getStringCellValue());

                        String dateStr = row.getCell(4).getStringCellValue();
                        uploadRow.setAwardDate(LocalDate.parse(dateStr));

                        rows.add(uploadRow);
                    }
                    return Flux.fromIterable(rows);
                },
                workbook -> {
                    try {
                        workbook.close();
                    } catch (IOException ignored) { }
                }
        );
    }
}
