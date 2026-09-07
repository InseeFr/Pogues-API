package fr.insee.pogues.persistence.repository;

import fr.insee.pogues.domain.entity.db.Version;
import fr.insee.pogues.utils.json.JSONFunctions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.UUID;

import static fr.insee.pogues.service.TimeService.convertTimestampToZonedDateTime;

@Slf4j
public class VersionRowMapper implements RowMapper<Version> {
    private boolean withData;
    private Clock clock;

    public VersionRowMapper(boolean withData, Clock clock){
        this.withData = withData;
        this.clock = clock;
    }

    @Override
    public Version mapRow(ResultSet rs, int rowNum) throws SQLException {
        Version version = new Version();
        version.setId(UUID.fromString(rs.getString("id")));
        version.setPoguesId(rs.getString("pogues_id"));
        version.setDay(rs.getDate("day"));
        version.setTimestamp(convertTimestampToZonedDateTime(rs.getTimestamp("timestamp"), clock));
        version.setAuthor(rs.getString("author"));
        if(withData){
            version.setData(JSONFunctions.jsonStringtoJsonNode(rs.getString("data")));
        }
        return version;
    }
}
