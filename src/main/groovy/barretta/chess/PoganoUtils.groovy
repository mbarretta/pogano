package barretta.chess

import barretta.utils.PropertyManager
import groovy.util.logging.Slf4j

@Slf4j
class PoganoUtils {
    static def writePgn(pgn, name, folder) {
        def outputDir = new File(PropertyManager.instance.properties.outputDir, folder)
        outputDir.mkdir()
        def outFile = new File(outputDir, "${name}.pgn")
        outFile.withWriter {
            it << pgn
        }
        log.info("Wrote [$outFile.absolutePath]")
    }
}
