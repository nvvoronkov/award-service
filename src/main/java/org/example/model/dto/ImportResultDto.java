package org.example.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class ImportResultDto {
    private int totalRows;
    private int importedRows;
    private int skippedRows;
    private List<ImportErrorDto> errors;
}