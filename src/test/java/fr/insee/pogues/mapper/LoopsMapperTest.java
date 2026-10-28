package fr.insee.pogues.mapper;

import fr.insee.pogues.model.DynamicIterationType;
import fr.insee.pogues.model.ExpressionType;
import fr.insee.pogues.model.dto.loops.LoopDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoopsMapperTest {

    @Test
    @DisplayName("Should map a loop based on a scope")
    void toModel_basedOn() {
        LoopDTO loop = new LoopDTO("L1", "BOUCLE1", "S1", "S2", "TABLE", "$AGE$ < 18",
                null, null, null, null, null);

        DynamicIterationType res = LoopsMapper.toModel(loop);

        assertThat(res.getId()).isEqualTo("L1");
        assertThat(res.getName()).isEqualTo("BOUCLE1");
        assertThat(res.getMemberReference()).isEqualTo(List.of("S1", "S2"));
        assertThat(res.getIterableReference()).isEqualTo("TABLE");
        assertThat(res.getFilter().getValue()).isEqualTo("$AGE$ < 18");
        assertThat(res.isIsFixedLength()).isNull();
        assertThat(res.getStep()).isNull();
        assertThat(res.getLabel()).isNull();
    }

    @Test
    @DisplayName("Should map a loop with a fixed number of occurrences")
    void toModel_fixedLength() {
        LoopDTO loop = new LoopDTO("L1", "BOUCLE1", "SS1", "SS1", null, null,
                true, "2", true, null, null);

        DynamicIterationType res = LoopsMapper.toModel(loop);

        assertThat(res.getMemberReference()).isEqualTo(List.of("SS1", "SS1"));
        assertThat(res.getIterableReference()).isNull();
        assertThat(res.isIsFixedLength()).isTrue();
        assertThat(res.getStep()).isEqualTo(BigInteger.ONE);
        assertThat(res.getSize().getValue()).isEqualTo("2");
        assertThat(res.isShouldSplitIterations()).isTrue();
        assertThat(res.getMinimum()).isNull();
        assertThat(res.getMaximum()).isNull();
        assertThat(res.getFilter()).isNull();
    }

    @Test
    @DisplayName("Should map a loop with a dynamic number of occurrences")
    void toModel_dynamicLength() {
        LoopDTO loop = new LoopDTO("L1", "BOUCLE1", "S1", "E1", null, null,
                false, null, null, "1", "$NB$");

        DynamicIterationType res = LoopsMapper.toModel(loop);

        assertThat(res.isIsFixedLength()).isFalse();
        assertThat(res.getStep()).isEqualTo(BigInteger.ONE);
        assertThat(res.getMinimum().getValue()).isEqualTo("1");
        assertThat(res.getMaximum().getValue()).isEqualTo("$NB$");
        assertThat(res.getLabel()).isNull();
        assertThat(res.getSize()).isNull();
        assertThat(res.isShouldSplitIterations()).isNull();
    }

    @Test
    @DisplayName("Should map back a loop based on a scope")
    void toDTO_basedOn() {
        LoopDTO loop = new LoopDTO("L1", "BOUCLE1", "S1", "S2", "TABLE", "$AGE$ < 18",
                null, null, null, null, null);

        assertThat(LoopsMapper.toDTO(LoopsMapper.toModel(loop))).isEqualTo(loop);
    }

    @Test
    @DisplayName("Should map back a loop with a fixed number of occurrences")
    void toDTO_fixedLength() {
        LoopDTO loop = new LoopDTO("L1", "BOUCLE1", "SS1", "SS1", null, null,
                true, "2", true, null, null);

        assertThat(LoopsMapper.toDTO(LoopsMapper.toModel(loop))).isEqualTo(loop);
    }

    @Test
    @DisplayName("Should map back a loop with a dynamic number of occurrences")
    void toDTO_dynamicLength() {
        LoopDTO loop = new LoopDTO("L1", "BOUCLE1", "S1", "E1", null, null,
                false, null, null, "1", "$NB$");

        assertThat(LoopsMapper.toDTO(LoopsMapper.toModel(loop))).isEqualTo(loop);
    }

    @Test
    @DisplayName("Should map a legacy loop, without isFixedLength and with irrelevant fields, to a valid shape")
    void toDTO_legacy() {
        // Given a loop based on a scope which also has a step and bounds, as saved by the former frontend
        DynamicIterationType basedOn = new DynamicIterationType();
        basedOn.setId("L1");
        basedOn.setName("BOUCLE1");
        basedOn.getMemberReference().addAll(List.of("S1", "S2"));
        basedOn.setIterableReference("TABLE");
        basedOn.setStep(BigInteger.ONE);
        basedOn.setMinimum(expression("1"));
        // And a loop with bounds but without isFixedLength
        DynamicIterationType dynamic = new DynamicIterationType();
        dynamic.setId("L2");
        dynamic.setName("BOUCLE2");
        dynamic.getMemberReference().addAll(List.of("S1", "S2"));
        dynamic.setMinimum(expression("1"));
        dynamic.setMaximum(expression("5"));

        // Then only the fields of the loop shape are kept
        assertThat(LoopsMapper.toDTO(basedOn)).isEqualTo(new LoopDTO("L1", "BOUCLE1", "S1", "S2", "TABLE", null,
                null, null, null, null, null));
        assertThat(LoopsMapper.toDTO(dynamic)).isEqualTo(new LoopDTO("L2", "BOUCLE2", "S1", "S2", null, null,
                false, null, null, "1", "5"));
    }

    private static ExpressionType expression(String value) {
        ExpressionType expression = new ExpressionType();
        expression.setValue(value);
        return expression;
    }

}
