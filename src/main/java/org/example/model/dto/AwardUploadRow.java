package org.example.model.dto;

import lombok.*;

import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor
public class AwardUploadRow {
    private Long employeeId;
    private String employeeFullName;
    private String awardCode;
    private String awardName;
    private LocalDate awardDate;
    private int rowNumber;
}
