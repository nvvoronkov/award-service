package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.dto.AwardUploadRow;
import org.example.model.dto.ImportErrorDto;
import org.example.model.dto.ImportResultDto;
import org.example.service.AwardImportService;
import org.example.service.parser.CsvAwardFileParser;
import org.example.service.parser.ExcelAwardFileParser;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/awards")
@RequiredArgsConstructor
public class AwardImportController {
    private final CsvAwardFileParser csvParser;
    private final ExcelAwardFileParser excelParser;
    private final AwardImportService importService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ImportResultDto>> importAwards(@RequestPart("file") FilePart filePart) {

        String filename = filePart.filename().toLowerCase(Locale.ROOT);
        Flux<AwardUploadRow> rowsFlux;

        if (filename.endsWith(".csv")) {
            rowsFlux = filePart.content()
                    .reduce(DataBuffer::write)
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return new ByteArrayInputStream(bytes);
                    })
                    .flatMapMany(csvParser::parse);
        } else if (filename.endsWith(".xlsx")) {
            rowsFlux = filePart.content()
                    .reduce(DataBuffer::write)
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return new ByteArrayInputStream(bytes);
                    })
                    .flatMapMany(excelParser::parse);
        } else {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(new ImportResultDto(0, 0, 0,
                            List.of(new ImportErrorDto(0, "Unsupported file format")))));
        }

        return importService.importAwards(rowsFlux)
                .map(result -> ResponseEntity.ok(result));
    }
}
