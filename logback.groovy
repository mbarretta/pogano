import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.*

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d %-5level [%thread] %logger{36} - %msg%n"
    }
}
logger("com.digitalreasoning", DEBUG)
root(INFO, ["CONSOLE"])
