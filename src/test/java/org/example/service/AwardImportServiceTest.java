package org.example.service;

import org.example.model.Award;
import org.example.model.Employee;
import org.example.model.dto.AwardUploadRow;
import org.example.model.dto.ImportErrorDto;
import org.example.model.dto.ImportResultDto;
import org.example.repository.AwardRepository;
import org.example.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwardImportServiceTest {

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    AwardRepository awardRepository;

    @InjectMocks
    AwardImportService awardImportService;

    @Test
    void shouldImportOnlyExistingEmployees() {
        AwardUploadRow row1 = new AwardUploadRow(1L, "Иванов И.И.", "A1", "Лучший сотрудник", LocalDate.parse("2024-01-10"), 2);
        AwardUploadRow row2 = new AwardUploadRow(2L, "Петров П.П.", "A2", "За вклад", LocalDate.parse("2024-01-11"), 3);

        Flux<AwardUploadRow> rows = Flux.just(row1, row2);

        when(employeeRepository.findAllById(Collections.singleton(any())))
                .thenReturn(Flux.just(new Employee(1L, "Иванов И.И.")));

        when(awardRepository.save(any(Award.class)))
                .thenAnswer(invocation -> {
                    Award a = invocation.getArgument(0);
                    a.setId(100L);
                    return Mono.just(a);
                });

        ImportResultDto result = awardImportService.importAwards(rows).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getImportedRows()).isEqualTo(1);
        assertThat(result.getSkippedRows()).isEqualTo(1);
        assertThat(result.getErrors())
                .hasSize(1)
                .first()
                .satisfies(error -> {
                    assertThat(error.getRowNumber()).isEqualTo(3);
                    assertThat(error.getMessage()).contains("not found");
                });

        verify(awardRepository, times(1)).save(any(Award.class));
    }

    @Test
    void shouldReturnZeroCountsForEmptyInput() {
        Flux<AwardUploadRow> emptyRows = Flux.empty();

        when(employeeRepository.findAllById((Iterable<Long>) any()))
                .thenReturn(Flux.empty());

        ImportResultDto result = awardImportService.importAwards(emptyRows).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRows()).isEqualTo(0);
        assertThat(result.getImportedRows()).isEqualTo(0);
        assertThat(result.getSkippedRows()).isEqualTo(0);
        assertThat(result.getErrors()).isEmpty();

        verify(employeeRepository, times(1)).findAllById((Iterable<Long>) any());
        verifyNoInteractions(awardRepository);
    }

    @Test
    void shouldSkipAllRowsWhenNoEmployeesExist() {
        AwardUploadRow row1 = new AwardUploadRow(10L, "Неизвестный", "X1", "Награда",
                LocalDate.parse("2024-02-01"), 2);
        AwardUploadRow row2 = new AwardUploadRow(11L, "Неизвестный 2", "X2", "Награда 2",
                LocalDate.parse("2024-02-02"), 3);

        Flux<AwardUploadRow> rows = Flux.just(row1, row2);

        when(employeeRepository.findAllById((Iterable<Long>) any()))
                .thenReturn(Flux.empty());

        ImportResultDto result = awardImportService.importAwards(rows).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getImportedRows()).isEqualTo(0);
        assertThat(result.getSkippedRows()).isEqualTo(2);
        assertThat(result.getErrors()).hasSize(2);

        verifyNoInteractions(awardRepository);
    }

    @Test
    void shouldCollectErrorWhenSaveFails() {
        AwardUploadRow row = new AwardUploadRow(1L, "Иванов И.И.", "A1", "Награда",
                LocalDate.parse("2024-03-01"), 2);

        Flux<AwardUploadRow> rows = Flux.just(row);

        when(employeeRepository.findAllById((Iterable<Long>) any()))
                .thenReturn(Flux.just(new Employee(1L, "Иванов И.И.")));

        when(awardRepository.save(any(Award.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        ImportResultDto result = awardImportService.importAwards(rows).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getImportedRows()).isEqualTo(0);
        assertThat(result.getSkippedRows()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);

        verify(awardRepository, times(1)).save(any(Award.class));
    }

    @Test
    void shouldHandleMultipleRowsWithMixedResults() {
        AwardUploadRow okRow = new AwardUploadRow(1L, "Иванов И.И.", "A1", "Награда ОК",
                LocalDate.parse("2024-04-01"), 2);
        AwardUploadRow missingEmployeeRow = new AwardUploadRow(2L, "Петров П.П.", "A2", "Награда Мимо",
                LocalDate.parse("2024-04-02"), 3);
        AwardUploadRow failingSaveRow = new AwardUploadRow(1L, "Иванов И.И.", "A3", "Награда Ошибка",
                LocalDate.parse("2024-04-03"), 4);

        Flux<AwardUploadRow> rows = Flux.just(okRow, missingEmployeeRow, failingSaveRow);

        when(employeeRepository.findAllById((Iterable<Long>) any()))
                .thenReturn(Flux.just(new Employee(1L, "Иванов И.И.")));

        when(awardRepository.save(any(Award.class)))
                .thenAnswer(invocation -> {
                    Award a = invocation.getArgument(0);
                    if ("A3".equals(a.getAwardCode())) {
                        return Mono.error(new RuntimeException("DB fail"));
                    }
                    a.setId(200L);
                    a.setCreatedAt(LocalDateTime.now());
                    return Mono.just(a);
                });

        ImportResultDto result = awardImportService.importAwards(rows).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRows()).isEqualTo(3);
        assertThat(result.getImportedRows()).isEqualTo(1);
        assertThat(result.getSkippedRows()).isEqualTo(2);

        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors())
                .extracting(ImportErrorDto::getRowNumber)
                .containsExactlyInAnyOrder(3, 4);
    }

    @Test
    void shouldNotFailIfEmployeeRepositoryReturnsDuplicateEmployees() {
        AwardUploadRow row = new AwardUploadRow(1L, "Иванов И.И.", "A1", "Награда",
                LocalDate.parse("2024-05-01"), 2);

        Flux<AwardUploadRow> rows = Flux.just(row);

        Employee e = new Employee(1L, "Иванов И.И.");
        when(employeeRepository.findAllById((Iterable<Long>) any()))
                .thenReturn(Flux.just(e, e));

        when(awardRepository.save(any(Award.class)))
                .thenAnswer(invocation -> {
                    Award a = invocation.getArgument(0);
                    a.setId(300L);
                    a.setCreatedAt(LocalDateTime.now());
                    return Mono.just(a);
                });

        ImportResultDto result = awardImportService.importAwards(rows).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getImportedRows()).isEqualTo(1);
        assertThat(result.getSkippedRows()).isEqualTo(0);
        assertThat(result.getErrors()).isEmpty();

        verify(awardRepository, times(1)).save(any(Award.class));
    }
}