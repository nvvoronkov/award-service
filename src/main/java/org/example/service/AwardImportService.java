package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.model.Award;
import org.example.model.Employee;
import org.example.model.dto.AwardUploadRow;
import org.example.model.dto.ImportErrorDto;
import org.example.model.dto.ImportResultDto;
import org.example.repository.AwardRepository;
import org.example.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AwardImportService {
    private final EmployeeRepository employeeRepository;
    private final AwardRepository awardRepository;

    public Mono<ImportResultDto> importAwards(Flux<AwardUploadRow> rows) {
        List<ImportErrorDto> errors = new ArrayList<>();
        AtomicInteger total = new AtomicInteger();
        AtomicInteger imported = new AtomicInteger();

        Mono<Set<Long>> existingEmployeeIdsMono = rows
                .map(AwardUploadRow::getEmployeeId)
                .collect(Collectors.toSet())
                .flatMap(ids ->
                        employeeRepository.findAllById(Flux.fromIterable(ids))
                                .map(Employee::getId)
                                .collect(Collectors.toSet())
                );

        return existingEmployeeIdsMono.flatMapMany(existingIds ->
                        rows.flatMap(row -> {
                                    total.incrementAndGet();

                                    if (!existingIds.contains(row.getEmployeeId())) {
                                        errors.add(ImportErrorDto.builder()
                                                .rowNumber(row.getRowNumber())
                                                .message("Employee with id " + row.getEmployeeId() + " not found")
                                                .build()
                                        );
                                        return Mono.empty();
                                    }

                                    Award award = new Award();
                                    award.setEmployeeId(row.getEmployeeId());
                                    award.setAwardCode(row.getAwardCode());
                                    award.setAwardName(row.getAwardName());
                                    award.setAwardDate(row.getAwardDate());
                                    award.setCreatedAt(LocalDateTime.now());

                                    return awardRepository.save(award)
                                            .doOnSuccess(saved -> imported.incrementAndGet())
                                            .onErrorResume(ex -> {
                                                errors.add(ImportErrorDto.builder()
                                                        .rowNumber(row.getRowNumber())
                                                        .message("Error with save award: " + ex.getMessage())
                                                        .build()
                                                );
                                                return Mono.empty();
                                            });
                                }
                        ))
                .then(Mono.fromSupplier(() -> {
                    int totalRows = total.get();
                    int importedRows = imported.get();
                    int skippedRows = totalRows - importedRows;
                    return ImportResultDto.builder()
                            .totalRows(totalRows)
                            .importedRows(importedRows)
                            .skippedRows(skippedRows)
                            .errors(errors)
                            .build();
                }));
    }
}
