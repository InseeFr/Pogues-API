package fr.insee.pogues.domain.entity.db;

import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationQuestionNumberingMode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStep;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "release_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseRequestDB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "pogues_version_id")
    private UUID poguesVersionId;

    @Column(name = "pogues_id")
    private String poguesId;

    @Column(name = "release_description")
    private String releaseDescription;

    @Column(name = "context")
    @Enumerated(EnumType.STRING)
    private GenerationContext context;

    @Column(name = "generation_params_question_numbering_mode")
    @Enumerated(EnumType.STRING)
    private GenerationQuestionNumberingMode generationParamsQuestionNumberingMode;

    @Column(name = "generation_params_response_time_question")
    private Boolean generationParamsResponseTimeQuestion;

    @Column(name = "current_step")
    @Enumerated(EnumType.STRING)
    private ReleaseRequestStep currentStep;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReleaseRequestStatus status;

    @Column(name = "status_description")
    @Enumerated(EnumType.STRING)
    private ReleaseRequestStatusCode statusDescription;

    @OneToMany(mappedBy = "releaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReleaseRequestModeDB> modes = new ArrayList<>();

    public void addMode(ReleaseRequestModeDB mode) {
        modes.add(mode);
        mode.setReleaseRequest(this);
    }

    public void removeMode(ReleaseRequestModeDB mode) {
        modes.remove(mode);
        mode.setReleaseRequest(null);
    }
}
