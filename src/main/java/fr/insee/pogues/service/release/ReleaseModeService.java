package fr.insee.pogues.service.release;

import fr.insee.pogues.domain.entity.db.ReleaseRequestModeDB;
import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.release.ReleaseRequestModeStep;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatus;
import fr.insee.pogues.domain.enums.release.ReleaseRequestStatusCode;
import fr.insee.pogues.exception.release.ReleaseRequestNotFoundException;
import fr.insee.pogues.persistence.repository.jpa.ReleaseRequestModeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ReleaseModeService {

    private final ReleaseRequestModeRepository releaseRequestModeRepository;

    public static ReleaseRequestModeDB initModeRequest(CollectMode mode) {
        ReleaseRequestModeDB modeDB = new ReleaseRequestModeDB();
        modeDB.setMode(mode);
        modeDB.setCurrentStep(ReleaseRequestModeStep.INIT);
        modeDB.setStatus(ReleaseRequestStatus.CREATED);
        modeDB.setStatusDescription(ReleaseRequestStatusCode.NOT_STARTED);
        return modeDB;
    }


    public void updateStatus(Long modeRequestId, ReleaseRequestStatus releaseRequestStatus) {
        ReleaseRequestModeDB requestModeDB = getReleaseRequestModeDBById(modeRequestId);
        requestModeDB.setStatus(releaseRequestStatus);
        requestModeDB.setStatusDescription(ReleaseRequestStatusCode.IN_PROGRESS);
        releaseRequestModeRepository.save(requestModeDB);
    }

    public void updateStep(Long modeRequestId, ReleaseRequestModeStep step) {
        ReleaseRequestModeDB requestModeDB = getReleaseRequestModeDBById(modeRequestId);
        requestModeDB.setCurrentStep(step);
        releaseRequestModeRepository.save(requestModeDB);
    }

    public void terminate(Long modeRequestId) {
        ReleaseRequestModeDB requestModeDB = getReleaseRequestModeDBById(modeRequestId);
        requestModeDB.setStatus(ReleaseRequestStatus.COMPLETED);
        requestModeDB.setStatusDescription(ReleaseRequestStatusCode.COMPLETED);
        requestModeDB.setCurrentStep(ReleaseRequestModeStep.FINISHED);
        releaseRequestModeRepository.save(requestModeDB);
    }

    /**
     * Updates the collection instrument ID for a release request
     */
    public void updateCollectionInstrumentId(Long modeRequestId, UUID collectionInstrumentId) {
        ReleaseRequestModeDB requestModeDB = getReleaseRequestModeDBById(modeRequestId);
        requestModeDB.setCollectionInstrumentId(collectionInstrumentId);
        releaseRequestModeRepository.save(requestModeDB);
    }


    public ReleaseRequestModeDB getReleaseRequestModeDBById(Long modeRequestId) {
        return releaseRequestModeRepository
                .findById(modeRequestId)
                .orElseThrow(() -> new ReleaseRequestNotFoundException(modeRequestId));
    }
}
