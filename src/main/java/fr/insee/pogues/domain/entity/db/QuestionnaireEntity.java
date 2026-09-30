package fr.insee.pogues.domain.entity.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;


@Entity
@Table(name = "pogues")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class QuestionnaireEntity {

    @Id
    private String id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    @Setter
    private String data;

    @Column(name = "owner", insertable = false, updatable = false)
    private String owner;
}
