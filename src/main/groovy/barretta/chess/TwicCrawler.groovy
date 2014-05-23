package barretta.chess

import barretta.utils.GroovyZipInputStream
import groovy.util.logging.Slf4j
import org.ccil.cowan.tagsoup.Parser

@Slf4j
class TwicCrawler {
    def config = new ConfigSlurper().parse(new File("twic.properties").toURI().toURL())
    def count = 0

    public static void main(String[] args) {
        new TwicCrawler().run()
    }

    def run() {
        assert config.zipsUrl, "url must be set"
        assert config.pgnFile, "pgnFile must be set"

        log.info("Starting...\nwill append to [${config.pgnFile}]")

        if (config.fetchHistory) {
            def startId = findFirstId()
            while (true) {
                try {
                    appendPgns(fetchAndExtractPgns(startId++))
                    log.info("pulled [$count] zips")
                } catch (IOException e) {
                    log.info("Last id found [${startId - 1}]")
                    break
                }
            }
        } else {
            startId = startId ?: findLastId()
            try {
                appendPgns(fetchAndExtractPgns(startId))
            } catch (IOException e) {
                log.error("unable to find file with id [$startId]")
            }
        }

        saveLastId(startId)
    }

    def fetchAndExtractPgns(id) {
        def address = "${config.zipsUrl}/twic${id}g.zip"
        log.trace("fetching [$address]")

        def zipStream = new GroovyZipInputStream(new URL(address).openStream())
        def entries = []
        zipStream.eachEntry { zip ->
            count++
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
        log.info("found latest file id [$lastId]")
        return lastId
    }

    def findFirstId() {
        def firstId = getZipList().min()
        log.info("found earliest file id [$firstId]")
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
        log.trace("writing to [${config.pgnFile}]")
        new File(config.pgnFile) << pgns.join("\n")
    }

    def saveLastId(id) {
        config.startId = ++id
        new File("twic.properties").withWriter {
            config.writeTo(it)
        }
    }
}
