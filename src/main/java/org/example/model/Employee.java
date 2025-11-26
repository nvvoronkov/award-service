package org.example.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode
@Table("employee")
public class Employee {

    @Id
    private Long id;

    @Column("full_name")
    private String fullName;
}
