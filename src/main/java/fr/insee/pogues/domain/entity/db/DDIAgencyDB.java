package fr.insee.pogues.domain.entity.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ddi_agency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DDIAgencyDB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "label")
    private String label;
}
