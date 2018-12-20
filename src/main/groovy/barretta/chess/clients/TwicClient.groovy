package barretta.chess.clients

import barretta.chess.Pogano
import barretta.chess.PoganoUtils
import barretta.utils.GroovyZipInputStream
import barretta.utils.PropertyManager
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.ccil.cowan.tagsoup.Parser

import static groovyx.gpars.GParsPool.withPool

@Slf4j
class TwicClient implements Pogano.PoganoClient {
    def config
    final static def TWIC_URL = "http://theweekinchess.com"

    static void main(String[] args) {
        new TwicClient().run()
    }

    TwicClient() {
        loadConfig()
    }

    /**
     * "main" method
     * @return
     */
    List run() {
        def games = []
        if (config.allHistory) {
            games = fetchHistory()
        } else if (config.allSince) {
            games = fetchHistory(config.id)
        } else if (config.latest) {
            games = fetchLatest(config.count ?: 0)
        } else {
            games << fetchId(config.id)
        }
        return games
    }

    def fetchLatest(count = 1) {
        def games = []
        zipList[count - 1].each {
            games << fetchId(it)
        }
        return games
    }

    /**
     * fetch a single file
     * @return
     */
    def fetchId(id) {
        def game
        try {
            PoganoUtils.writePgn(fetchAndExtractPgns(id).join("\n"), id, "twic")
            game = id
            PropertyManager.instance.setLastFetchedId(id, this)
        }
        catch (IOException e) {
            log.error("unable to find file with id [$id]")
        }
        return game
    }

    /**
     * fetch all available files
     * @return
     */
    def fetchHistory(startId = null) {
        log.info("fetching history")
        def games = []
        def firstId = startId ?: findFirstId()

        //find the min numbered ID on the server and start there if one not passed in
        def lastId = findLastId()               //max ID on server
        def count = 0

        log.info("...fetching from [$firstId] to [$lastId]")

        withPool {
            //loop until we've fetched each file between first and last
            while (count <= (lastId - firstId)) {
                def id = firstId + count++    //current id to fetch
                ({ PoganoUtils.writePgn(fetchAndExtractPgns(it).join("\n"), id, "twic") } as Closure).callAsync(id)
                games << id
            }
        }
        //save lastId for the next run
        PropertyManager.instance.setLastFetchedId(lastId, this)

        log.info("Pulled [$count] zips")
        return games
    }

    /**
     * get zip from the server and extract the PGNs from the zip
     * @param id - id of file
     * @return extracted entries
     */
    def fetchAndExtractPgns(id) {
        def address = "${config.twicUrl}/zips/twic${id}g.zip"
        log.info("fetching [$address]")

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

    /**
     * get max file id from server
     * @return id
     */
    def findLastId() {
        def lastId = getZipList().max()
        log.info("found latest file id [$lastId]")
        return lastId
    }

    /**
     * get min file id from server
     * @return id
     */
    def findFirstId() {
        def firstId = getZipList().min()
        log.info("found earliest file id [$firstId]")
        return firstId
    }

    /**
     * get list of all the files available on TWIC for download...very dependent on HTML structure of the page
     * NB: method is memoized to cache the results of the first call
     * @return list of file IDs
     */
    @Memoized
    def getZipList() {
        def html = new XmlSlurper(new Parser()).parse(new URL("$config.twicUrl/twic").openStream())
        return html.body.depthFirst().find { it.@class == 'results-table' }.tbody.tr[2..-1].collect {
            it.td[5].a.@href
        }.inject([]) { list, file ->
            file = file.text().trim()
            def matcher = file =~ /\d+/
            if (matcher.find()) {
                list << (matcher.group() as int)
            }
            return list
        }
    }

    /**
     * load pogano.properties, do some validation
     */
    private def loadConfig() {
        config = PropertyManager.instance.properties
        assert optionsValid(), "invalid options"
    }

    def optionsValid() {
        //set default values
        if (!config.twicUrl || config.twicUrl == "null") {
            config.twicUrl = TWIC_URL
        }

        //validate
        def valid = true
        if (config.allSince) {
            def name = this.class.simpleName
            if ((!config.id || config.id == "null") && (!config."$name".lastFetchedId || config."$name".lastFetchedId == "null")) {
                log.error("missing game id: provide via --id on CLI or '${name}.lastFetchedId' in pogano.properties")
                valid = false
            } else {
                config.id = config.id ?: config."$name".lastFetchedId
            }
        }
        return valid
    }
}
