package barretta.utils

import groovy.util.logging.Slf4j

@Slf4j
@Singleton(strict = false)
class PropertyManager {
    final String PROPERTIES_FILE = "pogano.properties"
    ConfigObject properties

    private PropertyManager() {
        reload()
    }

    def reload(path = PROPERTIES_FILE) {
        //try getting from the file system
        if (new File(path).exists()) {
            properties = new ConfigSlurper().parse(new File(path).toURI().toURL())
        }
        //else try getting from class path
        else if (GroovyClassLoader.getSystemResource(path) != null) {
            properties = new ConfigSlurper().parse(GroovyClassLoader.getSystemResource(path))
        }
        //else error
        else {
            throw new Exception("unable to find $path")
        }
    }

    @Override
    String toString() {
        return prettyPrint(properties).toString()
    }

    static def prettyPrint(ConfigObject properties, level = 1, sb = new StringBuilder()) {
        return properties.inject(sb) { s, name, value ->
            s.append("\n").append("\t" * level).append(name)
            if (!(value instanceof ConfigObject)) {
                return s.append("=").append(value)
            } else {
                return prettyPrint(properties.getProperty(name), level + 1, s)
            }
        }
    }

    def save() {
        new File(properties.getConfigFile().getPath()).withWriter {
            properties.writeTo(it)
        }
    }

    def setLastFetchedId(id, obj) {
        def prop = obj.class.simpleName + ".lastFetchedId"
        properties.setProperty(prop, id)

        //write only the changed 'lastFetchedId' field - we don't want to persist CLI stuff
        def f = new File(properties.getConfigFile().getPath())
        def text = f.text
        if (text.contains(prop)) {
            text = (text =~ /$prop=.*/).replaceFirst("$prop='$id'")
        } else {
            text += "\n$prop='$id'"
        }
        f.write(text)
    }

    private def isValid(ConfigObject config = this.properties) {
        def valid = true
        if (!config.isSet("outputDir")) {
            log.error("outputDir is required")
            valid = false
        } else if (!new File(config.outputDir as String).exists()) {
            log.warn("output directory [$config.outputDir] does not exist - creating")
            new File(config.outputDir as String).mkdirs()
        }
        if (!["fetchAll", "fetchTwic", "fetchLichess"].any { config.isSet(it) }) {
            log.error("One of fetchAll, fetchTwic, or fetchLichess must be set")
            valid = false
        }
        if (!["id", "allHistory", "allSince", "latest"].any { config.isSet(it) }) {
            log.error("One of id, latest, allHistory, or allSince must be set")
            valid = false
        }
        if (config.isSet("count") && !(config.count ==~ /[0-9]*/)) {
            log.error("--count must be an integer")
            valid = false
        }
        return valid
    }
}
