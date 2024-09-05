package barretta.chess.clients

import barretta.chess.Pogano
import barretta.chess.PoganoUtils
import barretta.utils.PropertyManager
import groovy.json.JsonSlurper
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import groovyx.gpars.GParsPool
import wslite.rest.RESTClient

import java.util.concurrent.ConcurrentLinkedQueue

@Slf4j
class LichessClient implements Pogano.PoganoClient {
    def client
    def config
    final static def LICHESS_URL = "https://lichess.org"

    LichessClient() {
        loadConfig()
        initClient()
    }

    List run() {
        def games = []

        if (optionsValid()) {
            if (config.allHistory) {
                games = fetchAllGames()
            } else if (config.allSince) {
                games = fetchHistory(config.id)
            } else if (config.latest) {
                //todo consider if this even needs to exist
                games = fetchLatest(config.count ?: 1)
            } else {
                games << fetchGame(config.id)
            }
        }
        return games
    }

    def fetchLatest(count = 1) {
        return fetchGames(getAllGameIds().take(count))
    }

    def fetchGames(ids) {
        log.info("fetching games [${ids.join(',')}]")
        def response = client.post(
            headers: [Authorization: "Bearer ${config.lichessApiToken}"],
            path: "/games/export/_ids"
        ) {
            text ids.join(",")
        }
        PropertyManager.instance.setLastFetchedId(ids.first(), this)

        def idQueue = new LinkedList(ids)
        new String(response.data).split("\n\n\n").each {
            PoganoUtils.writePgn(it, idQueue.remove(), "lichess")
        }
        return ids
    }

    /**
     * get one game by id
     * @param id - game id
     */
    def fetchGame(id) {
        log.info("fetching game [$id]")
        def game = null
        try {
            def response = client.get(
                path: "/game/export/${id}.pgn",
                headers: [
                    Authorization: "Bearer ${config.lichessApiToken}",
                    "User-Agent" : "" //why empty? see: https://github.com/ornicar/lila/issues/2331
                ]
            )

            PoganoUtils.writePgn(new String(response.data), id, "lichess")
            PropertyManager.instance.setLastFetchedId(id, this)
            game = id
        }
        catch (e) {
            log.error("Wuh oh: [$e.message] [$e.cause] for id [$id]")
        }
        return game
    }

    def fetchAllGames() {
        log.info("fetching all games")
        def games = []
        def ids = getAllGameIds() as LinkedList
        try {
            def response = client.get(
                headers: [ Authorization: "Bearer ${config.lichessApiToken}" ],
                readTimeout: 100000,
                path: "/api/games/user/${config.lichessUser}"
            )

            new String(response.data).split("\n\n\n").each {
                def id = ids.remove()
                PoganoUtils.writePgn(it, id,"lichess")
                games << id
            }
            PropertyManager.instance.setLastFetchedId(games.first(), this)
        } catch (e) {
            log.error("time to freak out: $e.message $e.cause")
        }
        log.info("...fetched [${games.size()}]")
        return games
    }

    def fetchHistory(startId = null) {
        log.info("fetching history")
        if (startId) {
            def ids = getAllGameIds()
            ids = ids[0..ids.indexOf(startId)]
            return fetchGames(ids)
        } else {
            return fetchAllGames()
        }
    }

    @Memoized
    def getAllGameIds() {
        log.info("getting list of all games...")
        def games = []
        try {
            def response = client.get(
                headers: [
                    Authorization: "Bearer ${config.lichessApiToken}",
                    Accept       : "application/x-ndjson"
                ],
                readTimeout: 100000,
                path: "/api/games/user/${config.lichessUser}",
                query: [moves: "false"]
            )
            games = new String(response.data).readLines().collect {
                new JsonSlurper().parseText(it).id
            }
        } catch (e) {
            log.error("time to freak out: $e.message $e.cause")
        }
        log.info("...found [${games.size()}]")
        return games
    }

    private def loadConfig() {
        config = PropertyManager.instance.properties
    }

    private def initClient() {
        //set default values
        if (!config.lichessUrl || config.lichessUrl == "null") {
            client = new RESTClient(LICHESS_URL)
        } else {
            client = new RESTClient(config.lichessUrl)
        }
        client.httpClient.sslTrustAllCerts = true
    }

    def optionsValid() {
        def valid = true
        if (!config.lichessUser || config.lichessUser == "null") {
            log.error("missing username: provide via --lichessUser on CLI or as 'lichessUser' in pogano.properties")
            valid = false
        }
        if (!config.lichessApiToken || config.lichessApiToken == "null") {
            log.error("missing lichess API token: provide via --lichessApiToken on CLI or as 'lichessApiToken' in pogano.properties")
            valid = false
        }
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
