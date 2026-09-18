package fr.insee.pogues.domain.entity.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "serie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalSerieDB {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "uri")
    private String uri;

    @Column(name = "label")
    private String label;

    @Column(name = "alt_label")
    private String altLabel;
}
