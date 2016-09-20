package barretta.utils

import groovy.util.logging.Slf4j

@Slf4j
class PropertyManager {
	final ConfigObject properties
	final File propFile

	def PropertyManager(fileName = "twic.properties") {
		propFile = new File(fileName)

		//try getting from the file system
		if (propFile.exists()) {
			properties = new ConfigSlurper().parse(propFile.toURI().toURL())
		}
		//else try getting from class path
		else if ((propFile = GroovyClassLoader.getSystemResource(fileName)) != null) {
			properties = new ConfigSlurper().parse(propFile)
		}
		//else error
		else {
			throw new Exception("unable to find $fileName")
		}
	}

	@Override
	public String toString() {
		return prettyPrint(properties).toString()
	}

	public static def prettyPrint(ConfigObject properties, level = 1, sb = new StringBuilder()) {
		return properties.inject(sb) { s, name, value ->
			s.append("\n").append("\t" * level).append(name)
			if (!(value instanceof ConfigObject)) {
				return s.append("=").append(value)
			} else {
				return prettyPrint(properties.getProperty(name), level + 1, s)
			}
		}
	}

	public def save() {
		propFile.withWriter {
			properties.writeTo(it)
		}
	}
}
