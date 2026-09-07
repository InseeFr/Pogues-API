package fr.insee.pogues.service.release.context;

import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.parameters.GenerationParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
public class ReleaseModeWorkflowContext {

    private final ReleaseWorkflowContext releaseContext;
    private final Long releaseRequestModeId;
    private final CollectMode mode;
    @Setter
    private GenerationParameters generationParameters;
    @Setter private String lunatic;
    @Setter private UUID collectionInstrumentId;

    public ReleaseModeWorkflowContext(ReleaseWorkflowContext releaseContext,
                                      Long releaseRequestModeId,
                                      CollectMode mode) {
        this.releaseContext = releaseContext;
        this.releaseRequestModeId = releaseRequestModeId;
        this.mode = mode;
    }

    public void clearLunatic() {
        this.lunatic = null;
    }
}
