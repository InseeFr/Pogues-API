package fr.insee.pogues.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


@Component
@AllArgsConstructor
public class TimeService {

    private final Clock clock;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
     * This function is used to get the date in ISO 8601 format Date
     * @param instant (can be null)
     * @return if date (Instant) is provided, it returns formated Date, if not returns formated of now.
     */
    public String getIsoDateFromInstant(Instant instant){
        if(instant != null) {
            return instant.atZone(clock.getZone()).format(formatter);
        }
        ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(clock);
        return zonedDateTimeNow.format(formatter);
    }

    public String getIsoDateFromInstantNow(){
        return ZonedDateTime.now(clock).format(formatter);
    }


    public static Timestamp convertZonedDateTimeToTimestamp(ZonedDateTime zonedDateTime){
        return Timestamp.from(zonedDateTime.toInstant());
    }

    public static ZonedDateTime convertTimestampToZonedDateTime(Timestamp timestamp, Clock clock){
        return timestamp.toInstant().atZone(clock.getZone());
    }
}
