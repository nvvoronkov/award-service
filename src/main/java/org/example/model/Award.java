package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode
@Table("award")
public class Award {

    @Id
    private Long id;

    @Column("employee_id")
    private Long employeeId;

    @Column("award_code")
    private String awardCode;

    @Column("award_name")
    private String awardName;

    @Column("award_date")
    private LocalDate awardDate;

    @Column("created_at")
    private LocalDateTime createdAt;
}
