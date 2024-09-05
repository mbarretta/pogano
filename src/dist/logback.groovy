import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d %-5level [%thread] %logger{36} - %msg%n"
    }
}
root(INFO, ["CONSOLE"])
