package fr.insee.pogues.domain.enums.generation.parameters;

import fr.insee.pogues.domain.enums.generation.CollectMode;
import fr.insee.pogues.domain.enums.generation.GenerationContext;
import fr.insee.pogues.domain.enums.generation.GenerationFormat;
import fr.insee.pogues.domain.enums.generation.GenerationQuestionNumberingMode;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GenerationParameters {
    private final GenerationContext context;
    private final CollectMode modeParameter;
    private final GenerationFormat outFormat;
    private final String campaignName;
    private final String language;
    private final boolean identificationQuestion;
    @Setter
    private Boolean responseTimeQuestion;
    private final boolean commentSection;
    private final boolean sequenceNumbering;
    @Setter
    private GenerationQuestionNumberingMode questionNumberingMode;
    private final boolean arrowCharInQuestions;
    private final List<CollectMode> selectedModes;
    private final LunaticParameters lunatic;
}