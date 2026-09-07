package fr.insee.pogues.domain.entity.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import tools.jackson.databind.JsonNode;


@Entity
@Table(name = "pogues")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class QuestionnaireEntity {

    @Id
    private String id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private String data;
}
