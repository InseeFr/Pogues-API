package fr.insee.pogues.domain.entity.db;

import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Entity
@Table(name = "release_request_mode")
@Getter
@Setter
public class ReleaseRequestModeDB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_request_id", nullable = false)
    private ReleaseRequestDB releaseRequest;

    @Column(name = "mode")
    @Enumerated(EnumType.STRING)
    private CollectMode mode;

    @Enumerated(EnumType.STRING)
    private ReleaseRequestStatus status;

    @Column(name = "status_description")
    @Enumerated(EnumType.STRING)
    private ReleaseRequestStatusCode statusDescription;

    /** Étape par mode : BUILD_PARAMETERS, PUBLISH_PRERELEASE, GENERATE_LUNATIC, PUBLISH_LUNATIC. */
    @Column(name = "current_step")
    @Enumerated(EnumType.STRING)
    private ReleaseRequestModeStep currentStep;

    @Column(name = "collection_instrument_id")
    private UUID collectionInstrumentId;
}
