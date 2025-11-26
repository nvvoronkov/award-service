package org.example.controller;

import org.example.model.dto.ImportErrorDto;
import org.example.model.dto.ImportResultDto;
import org.example.service.AwardImportService;
import org.example.service.parser.CsvAwardFileParser;
import org.example.service.parser.ExcelAwardFileParser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AwardImportController.class)
@Import({CsvAwardFileParser.class, ExcelAwardFileParser.class})
class AwardImportControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AwardImportService awardImportService;

    @Test
    void shouldReturnBadRequestForUnsupportedFileExtension() {
        ImportResultDto expectedResult = new ImportResultDto(0, 0, 0,
                List.of(new ImportErrorDto(0, "Unsupported file format")));

        webTestClient.post()
                .uri("/api/v1/awards/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file",
                        new ClassPathResource("test-data/awards.txt")))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ImportResultDto.class)
                .value(result ->
                        assertThat(result.getErrors())
                                .extracting(ImportErrorDto::getMessage)
                                .anyMatch(msg -> msg.contains("Unsupported")));
    }

    @Test
    void shouldReturnImportResultFromService() {
        ImportResultDto mockResult = new ImportResultDto(
                2, 2, 0, Collections.emptyList());

        when(awardImportService.importAwards(any()))
                .thenReturn(Mono.just(mockResult));

        ClassPathResource csvResource = new ClassPathResource("test-data/awards.csv");

        webTestClient.post()
                .uri("/api/v1/awards/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", csvResource))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ImportResultDto.class)
                .value(result -> {
                    assertThat(result.getTotalRows()).isEqualTo(2);
                    assertThat(result.getImportedRows()).isEqualTo(2);
                    assertThat(result.getErrors()).isEmpty();
                });
    }

    @Test
    void shouldReturnBadRequestWhenFilePartIsMissing() {
        webTestClient.post()
                .uri("/api/v1/awards/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(new MultipartBodyBuilder().build()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnOkForExcelFile() {
        ImportResultDto mockResult = new ImportResultDto(
                3, 3, 0, Collections.emptyList());

        when(awardImportService.importAwards(any()))
                .thenReturn(Mono.just(mockResult));

        ClassPathResource xlsxResource = new ClassPathResource("test-data/awards.xlsx");

        webTestClient.post()
                .uri("/api/v1/awards/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", xlsxResource))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ImportResultDto.class)
                .value(result -> {
                    assertThat(result.getTotalRows()).isEqualTo(3);
                    assertThat(result.getImportedRows()).isEqualTo(3);
                });
    }

    @Test
    void shouldPropagateServiceErrorAsServerError() {
        when(awardImportService.importAwards(any()))
                .thenReturn(Mono.error(new RuntimeException("Service fail")));

        ClassPathResource csvResource = new ClassPathResource("test-data/awards.csv");

        webTestClient.post()
                .uri("/api/v1/awards/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", csvResource))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldCallServiceWithFluxOfRows() {
        ImportResultDto mockResult = new ImportResultDto(
                1, 1, 0, Collections.emptyList());

        ArgumentCaptor<Flux<?>> captor = ArgumentCaptor.forClass(Flux.class);

        when(awardImportService.importAwards(any()))
                .thenReturn(Mono.just(mockResult));

        ClassPathResource csvResource = new ClassPathResource("test-data/awards_single.csv");

        webTestClient.post()
                .uri("/api/v1/awards/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", csvResource))
                .exchange()
                .expectStatus().isOk();

        verify(awardImportService).importAwards((Flux) captor.capture());
        Flux<?> flux = captor.getValue();
        long count = flux.count().block();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void shouldReturnDetailedErrorsFromService() {
        ImportErrorDto error1 = new ImportErrorDto(2, "Employee not found");
        ImportErrorDto error2 = new ImportErrorDto(3, "Error save reward");

        ImportResultDto mockResult = new ImportResultDto(
                3, 1, 2, List.of(error1, error2));

        when(awardImportService.importAwards(any()))
                .thenReturn(Mono.just(mockResult));

        ClassPathResource csvResource = new ClassPathResource("test-data/awards_with_errors.csv");

        webTestClient.post()
                .uri("/api/v1/awards/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", csvResource))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ImportResultDto.class)
                .value(result -> {
                    assertThat(result.getTotalRows()).isEqualTo(3);
                    assertThat(result.getImportedRows()).isEqualTo(1);
                    assertThat(result.getErrors()).hasSize(2);
                    assertThat(result.getErrors())
                            .extracting(ImportErrorDto::getMessage)
                            .contains("Employee not found", "Error save reward");
                });
    }
}
