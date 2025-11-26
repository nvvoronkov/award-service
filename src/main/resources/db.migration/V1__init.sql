CREATE TABLE employee (
    id          BIGINT PRIMARY KEY,
    full_name   VARCHAR(255) NOT NULL
);

CREATE TABLE award (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employee (id),
    award_code      VARCHAR(100) NOT NULL,
    award_name      VARCHAR(255) NOT NULL,
    award_date      DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_award_employee ON award (employee_id);

ALTER TABLE award
    ADD CONSTRAINT uq_award_employee_code_date
        UNIQUE (employee_id, award_code, award_date);