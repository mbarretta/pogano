package barretta.chess

import barretta.chess.clients.TwicClient
import barretta.utils.PropertyManager
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

class TwicClientSpec extends Specification {
    @Shared
    def client

    def setupSpec() {
        def tmpDir = File.createTempDir()
        tmpDir.deleteOnExit()

        PropertyManager.instance.properties.outputDir = tmpDir.absolutePath
        client = new TwicClient()
    }

    def 'validates configuration: all since id'() {
        when: 'fetching allSince without an id'
        client.config.allSince = true
        client.config.TwicClient.lastFetchedId = null
        client.config.id = null

        then: 'options are invalid'
        !client.optionsValid()
    }

    def 'validates configuration: uses lastFetchedId when id is not set'() {
        when: 'fetching allSince without an id, but with a lastFetchedId'
        client.config.allSince = true
        client.config.TwicClient.lastFetchedId = 1000
        client.config.id = null

        then: 'options are valid and id is set'
        client.optionsValid()
        client.config.id == 1000
    }

    def 'can fetch a game by id'() {
        setup:
        def id = 1000

        when:
        def game = client.fetchId(id)

        then:
        game == id
        new File(client.config.outputDir + "/twic", "${id}.pgn").exists()
    }

    def 'log error when fetching non-existent id'() {
        setup:
        def id = "asdfasdfasdf"

        when:
        def game = client.fetchId(id)

        then:
        game == null
        notThrown(Exception)
    }

    def 'log error when fetching null id'() {
        setup:
        def id = null

        when:
        def game = client.fetchId(id)

        then:
        game == null
        notThrown(Exception)
    }


    def 'can fetch all game ids'() {
        expect:
        client.getZipList().size() >= 225
    }

    @Ignore
    def 'can fetch all games'() {
        expect:
        client.getZipList().size() == client.fetchHistory().size()
    }

    def 'can fetch all games since a given id'() {
        setup:
        def id = 1256

        when:
        def games = client.fetchHistory(id)

        then:
        !games.contains(id-1)
        games.size() == client.findLastId() - id + 1
    }
}
