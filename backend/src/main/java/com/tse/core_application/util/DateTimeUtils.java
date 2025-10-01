package com.tse.core_application.util;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class for handling timezone conversions between user timezone and server timezone.
 *
 * Rules:
 * - All incoming LocalDateTime/LocalTime from requests are in USER timezone
 * - All data stored in DB is in SERVER timezone
 * - All processing is done in SERVER timezone
 * - All outgoing LocalDateTime/LocalTime in responses are converted to USER timezone
 */
public class DateTimeUtils {

    /**
     * @deprecated This method is used to get the equivalent milliseconds of date and time as per the given timezone. This method is used in stats algo to get the
     * equivalent milliseconds of the date and time as per UTC time zone. Now, in stats algo, every comparison of date and time will be done as per the server
     * timezone and therefore there is no need to explicitly pass the timezone. Hence, in stats algo, this method is replaced by getDateAndTimeInMillis() method.
     */
    @Deprecated(since = "2022-07-29")
    public static Long getTimeStampForSQLDateAndTimeMS(LocalDateTime date, LocalTime time, String timeZone) {
//        2022-06-23: discussed between ankit and sir: Now all time are already in UTC. So we can just convert them in milliseconds.
//         timeZone = timeZone!=null?timeZone: Calendar.getInstance().getTimeZone().getID();
        ZoneOffset zoneOffset = ZoneId.of(timeZone).getRules().getOffset(Instant.now());

//        The output of this method is always in epochSeconds (epochSeconds * 1000 gets converted into milliseconds). Also, epochSeconds are always in UTC. When we give a timeZone
//        +0530 that gets added to the epochSeconds and if it is -0500 which is New york the equivalent seconds get subtracted.
        if (date != null && time != null) {
            return time.toEpochSecond(date.toLocalDate(), zoneOffset) * 1000;
        }
        return null;
    }

    /**
     * This method is used to get the equivalent milliseconds of date and time as per the server timezone. Suppose, the server is in Germany then the equivalent
     * milliseconds of date and time will come w.r.t Germany timezone.
     */
    public static Long getDateAndTimeInMillis(LocalDateTime date, LocalTime time) {
        long millis = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return millis;
    }

    /**
     * @deprecated this method was used to convert the server time into the UTC time. There is no need to convert the time explicitly
     * from server timezone to UTC timeZone
     */
    @Deprecated(since = "2022-07-19")
    public static Time getLocalTimeToUTCTime(Time time) {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        simpleDateFormat.setTimeZone(timeZone);
        String UTCTimeString = simpleDateFormat.format(time);
        Time UTCTime = java.sql.Time.valueOf(UTCTimeString);
        return UTCTime;
    }

    public static LocalDateTime convertToLocalDateTimeViaMillisecond(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static LocalDateTime getLocalCurrentDate() {
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        String formattedCurrentDateStr = dateFormatter.format(currentDate);
        LocalDateTime formattedCurrentDate = LocalDateTime.parse(formattedCurrentDateStr);
        return formattedCurrentDate;
    }

    public static java.time.LocalTime getLocalCurrentTime() {
        LocalTime localTime = LocalTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        Time formattedCurrentTime = Time.valueOf(timeFormatter.format(localTime));
        return formattedCurrentTime.toLocalTime();
    }

    /**
     * Converts user timezone date/time to server timezone.
     * Use this method when receiving LocalDateTime from API requests.
     *
     * @param localDateToConvert LocalDateTime in user timezone
     * @param localTimeZone User's timezone (e.g., "America/New_York", "Asia/Kolkata")
     * @return LocalDateTime in server timezone
     */
    public static LocalDateTime convertUserDateToServerTimezone(LocalDateTime localDateToConvert, String localTimeZone) {
        if (localDateToConvert == null) return null;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ZoneId headerTimeZone = ZoneId.of(localTimeZone);
        String stringDate = localDateToConvert.format(timeFormatter);
        LocalDateTime localDateTime = LocalDateTime.parse(stringDate, timeFormatter);
        ZonedDateTime zonedDateTimeInLocalTimeZone = ZonedDateTime.of(localDateTime, headerTimeZone);
        ZonedDateTime zonedDateTimeInServerTimeZone = zonedDateTimeInLocalTimeZone.withZoneSameInstant(ZoneId.of(String.valueOf(ZoneId.systemDefault())));
        LocalDateTime localDateTimeInServerTimeZone = LocalDateTime.from(zonedDateTimeInServerTimeZone);
        LocalDate onlyLocalDate = localDateTimeInServerTimeZone.toLocalDate();
        LocalTime onlyLocalTime = localDateTimeInServerTimeZone.toLocalTime();
        return LocalDateTime.of(onlyLocalDate, onlyLocalTime);
    }

    /**
     * Converts server timezone date/time to user timezone.
     * Use this method when sending LocalDateTime in API responses.
     *
     * @param input Server timezone LocalDateTime or Timestamp
     * @param localTimeZone User's timezone (e.g., "America/New_York", "Asia/Kolkata")
     * @return LocalDateTime in user timezone
     */
    public static LocalDateTime convertServerDateToUserTimezone(Object input, String localTimeZone) {
        if (input == null || localTimeZone == null) return null;
        ZoneId userZone = ZoneId.of(localTimeZone);
        if (input instanceof Timestamp) {
            Instant instant = ((Timestamp) input).toInstant();
            return instant.atZone(userZone).toLocalDateTime();
        }
        if (input instanceof LocalDateTime) {
            ZoneId serverZone = ZoneId.systemDefault();
            ZonedDateTime serverZoned = ((LocalDateTime) input).atZone(serverZone);
            return serverZoned.withZoneSameInstant(userZone).toLocalDateTime();
        }
        return null;
    }

    /**
     * Converts user timezone date/time to server timezone including seconds, milliseconds & microseconds.
     * Use this method when receiving precise LocalDateTime from API requests.
     *
     * @param localDateToConvert LocalDateTime in user timezone
     * @param localTimeZone User's timezone
     * @return LocalDateTime in server timezone with precision
     */
    public static LocalDateTime convertUserDateToServerTimezoneWithSeconds(LocalDateTime localDateToConvert, String localTimeZone) {
        if (localDateToConvert == null) return null;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnn");
        ZoneId headerTimeZone = ZoneId.of(localTimeZone);
        String stringDate = localDateToConvert.format(timeFormatter);
        LocalDateTime localDateTime = LocalDateTime.parse(stringDate, timeFormatter);
        ZonedDateTime zonedDateTimeInLocalTimeZone = ZonedDateTime.of(localDateTime, headerTimeZone);
        ZonedDateTime zonedDateTimeInServerTimeZone = zonedDateTimeInLocalTimeZone.withZoneSameInstant(ZoneId.of(String.valueOf(ZoneId.systemDefault())));
        LocalDateTime localDateTimeInServerTimeZone = LocalDateTime.from(zonedDateTimeInServerTimeZone);
        LocalDate onlyLocalDate = localDateTimeInServerTimeZone.toLocalDate();
        LocalTime onlyLocalTime = localDateTimeInServerTimeZone.toLocalTime();
        return LocalDateTime.of(onlyLocalDate, onlyLocalTime);
    }

    /**
     * Converts server timezone date/time to user timezone including seconds, milliseconds & microseconds.
     * Use this method when sending precise LocalDateTime in API responses.
     *
     * @param serverDateToConvert LocalDateTime in server timezone
     * @param localTimeZone User's timezone
     * @return LocalDateTime in user timezone with precision
     */
    public static LocalDateTime convertServerDateToUserTimezoneWithSeconds(LocalDateTime serverDateToConvert, String localTimeZone) {
        if (serverDateToConvert == null) return null;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnn");
        ZoneId serverTimeZone = ZoneId.of(String.valueOf(ZoneId.systemDefault()));
        String stringDate = serverDateToConvert.format(timeFormatter);
        LocalDateTime localDateTime = LocalDateTime.parse(stringDate, timeFormatter);
        ZonedDateTime zonedDateTimeInServerTimeZone = ZonedDateTime.of(localDateTime, serverTimeZone);
        ZonedDateTime zonedDateTimeInLocalTimeZone = zonedDateTimeInServerTimeZone.withZoneSameInstant(ZoneId.of(localTimeZone));
        LocalDateTime localDateTimeInLocalTimeZone = LocalDateTime.from(zonedDateTimeInLocalTimeZone);
        LocalDate onlyLocalDate = localDateTimeInLocalTimeZone.toLocalDate();
        LocalTime onlyLocalTime = localDateTimeInLocalTimeZone.toLocalTime();
        return LocalDateTime.of(onlyLocalDate, onlyLocalTime);
    }

    /**
     * Converts user timezone time to server timezone.
     * Use this method when receiving LocalTime from API requests.
     *
     * @param localTimeToConvert LocalTime in user timezone
     * @param localTimeZone User's timezone
     * @return LocalTime in server timezone
     */
    public static LocalTime convertUserTimeToServerTimeZone(LocalTime localTimeToConvert, String localTimeZone) {
        if (localTimeToConvert == null) return null;

        ZoneId zoneId = ZoneId.of(localTimeZone); // Get the ZoneId object for the time zone
        ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDate.now(), localTimeToConvert, zoneId); // Combine the local time with the time zone to create a ZonedDateTime
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalTime(); // Convert the ZonedDateTime to the server time
    }

    /**
     * Converts server timezone time to user timezone.
     * Use this method when sending LocalTime in API responses.
     *
     * @param serverTimeToConvert LocalTime in server timezone
     * @param localTimeZone User's timezone
     * @return LocalTime in user timezone
     */
    public static LocalTime convertServerTimeToUserTimeZone(LocalTime serverTimeToConvert, String localTimeZone) {
        if (serverTimeToConvert == null) return null;

        ZonedDateTime serverZonedDateTime = ZonedDateTime.of(LocalDate.now(), serverTimeToConvert, ZoneId.systemDefault()); // Combine the server time with the server's default time zone to create a ZonedDateTime
        ZonedDateTime zonedDateTime = serverZonedDateTime.withZoneSameInstant(ZoneId.of(localTimeZone)); // Convert the ZonedDateTime to the desired time zone
        return zonedDateTime.toLocalTime(); // Convert the ZonedDateTime to a LocalTime in the desired time zone
    }
}
