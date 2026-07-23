package fr.insee.pogues.client.metadata.mock;

import fr.insee.pogues.client.metadata.DDIASClient;
import fr.insee.pogues.client.metadata.model.ddias.Unit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "feature.metadata.ddias-client", havingValue = "mock")
public class DDIASMockClient implements DDIASClient {
    @Override
    public List<Unit> getUnits() {
        return List.of(
                new Unit("http://id.insee.fr/unit/euro", "€"),
                new Unit("http://id.insee.fr/unit/keuro", "k€"),
                new Unit("http://id.insee.fr/unit/percent", "%"),
                new Unit("http://id.insee.fr/unit/heure", "heures"),
                new Unit("http://id.insee.fr/unit/jour", "jours"),
                new Unit("http://id.insee.fr/unit/semaine", "semaines"),
                new Unit("http://id.insee.fr/unit/mois", "mois"),
                new Unit("http://id.insee.fr/unit/annee", "années"),
                new Unit("http://id.insee.fr/unit/an", "ans"),
                new Unit("http://id.insee.fr/unit/watt", "W"),
                new Unit("http://id.insee.fr/unit/kilowatt", "kW"),
                new Unit("http://id.insee.fr/unit/megawatt", "MW"),
                new Unit("http://id.insee.fr/unit/megawattheurepcs", "MWh PCS"),
                new Unit("http://id.insee.fr/unit/megawattheure", "MWh"),
                new Unit("http://id.insee.fr/unit/megawattpcs", "MW PCS"),
                new Unit("http://id.insee.fr/unit/kilowattthermique", "kWth"),
                new Unit("http://id.insee.fr/unit/kg", "kg"),
                new Unit("http://id.insee.fr/unit/tonne", "tonnes"),
                new Unit("http://id.insee.fr/unit/tonnematiereseche", "tonnes matières sèches"),
                new Unit("http://id.insee.fr/unit/degrecelsius", "°C"),
                new Unit("http://id.insee.fr/unit/bar", "bars"),
                new Unit("http://id.insee.fr/unit/litre", "litres"),
                new Unit("http://id.insee.fr/unit/metre", "mètres"),
                new Unit("http://id.insee.fr/unit/centimetre", "centimètres"),
                new Unit("http://id.insee.fr/unit/metrecarre", "mètres carrés")
        );
    }
}
