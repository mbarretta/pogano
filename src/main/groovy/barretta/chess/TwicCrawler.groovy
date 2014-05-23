package barretta.chess

import barretta.utils.GroovyZipInputStream
import groovy.util.logging.Slf4j
import org.ccil.cowan.tagsoup.Parser

@Slf4j
class TwicCrawler {
    def config = new ConfigSlurper().parse(new File("twic.properties").toURI().toURL())

    public static void main(String[] args) {
        new TwicCrawler().run()
    }

    def run() {
        assert config.zipsUrl, "url must be set"
        assert config.pgnFile, "pgnFile must be set"

        log.info("Starting...will append to [${config.pgnFile}]")

        if (config.fetchHistory) {
            fetchHistory()
        } else {
            fetchId()
        }

        log.info("Done")
    }

    def fetchId() {
       def startId = config.fetchId ?: findLastId()
        try {
            appendPgns(fetchAndExtractPgns(startId))
            saveLastId(startId)
        } catch (IOException e) {
            log.error("Unable to find file with id [$startId]")
        }
    }

    def fetchHistory() {
        def startId = findFirstId()
        def count = 0
        while (true) {
            try {
                appendPgns(fetchAndExtractPgns(startId++))
                saveLastId(startId)
                count++
            } catch (IOException e) {
                log.info("Last id found [${startId - 1}]")
                break
            }
        }
        log.info("Pulled [$count] zips")
    }

    def fetchAndExtractPgns(id) {
        def address = "${config.zipsUrl}/twic${id}g.zip"
        log.info("Fetching [$address]")

        def zipStream = new GroovyZipInputStream(new URL(address).openStream())
        def entries = []
        zipStream.eachEntry { zip ->
            def entry = new StringBuilder()
            zip.eachLine {
                entry.append(it).append("\n")
            }
            entries << entry.toString()
        }
        return entries
    }

    def findLastId() {
        def lastId = getZipList().max()
        log.info("Found latest file id [$lastId]")
        return lastId
    }

    def findFirstId() {
        def firstId = getZipList().min()
        log.info("Found earliest file id [$firstId]")
        return firstId
    }

    def getZipList() {
        def html = new XmlSlurper(new Parser()).parse(new URL(config.zipsUrl).openStream())
        return html.body.ul.li.inject([]) { list, file ->
            file = file.text().trim()
            def matcher = file =~ /\d+/
            if (matcher.find()) {
                list << (matcher.group() as int)
            }
            return list
        }
    }

    def appendPgns(pgns) {
        log.trace("Writing to [${config.pgnFile}]")
        new File(config.pgnFile) << pgns.join("\n")
    }

    def saveLastId(id) {
        config.fetchId = ++(id as int)
        new File("twic.properties").withWriter {
            config.writeTo(it)
        }
    }
}
