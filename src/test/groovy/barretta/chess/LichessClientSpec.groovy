package barretta.chess

import barretta.chess.clients.LichessClient
import barretta.utils.PropertyManager
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

class LichessClientSpec extends Specification
{
	@Shared
	def client = new LichessClient()
	@Shared
	def config = PropertyManager.instance.properties

	def setupSpec()
	{
		def tmpDir = File.createTempDir()
		tmpDir.deleteOnExit()

		config.outputDir = tmpDir.absolutePath

		client = new LichessClient()
	}

	def 'validates configuration: user'()
	{
		setup:
		def dirtyClient = new LichessClient()

		when: 'no user is set'
		dirtyClient.config.lichessUser = null

		then: 'options are invalid'
		!dirtyClient.optionsValid()
	}

	def 'validates configuration: all since id'()
	{
		setup:
		def dirtyClient = new LichessClient()

		when: 'fetching allSince without an id'
		dirtyClient.config.allSince = true
		dirtyClient.config.LichessClient.lastFetchedId = null
		dirtyClient.config.id = null

		then: 'options are invalid'
		!dirtyClient.optionsValid()
	}

	def 'validates configuration: uses lastFetchedId when id is not set'()
	{
		setup:
		def dirtyClient = new LichessClient()

		when: 'fetching allSince without an id, but with a lastFetchedId'
		dirtyClient.config.lichessUser = "Barretta"
		dirtyClient.config.allSince = true
		dirtyClient.config.LichessClient.lastFetchedId = "Z4JdhjBZ"
		dirtyClient.config.id = null

		then: 'options are valid and id is set'
		dirtyClient.optionsValid()
		dirtyClient.config.id == "Z4JdhjBZ"
	}

	def 'can fetch a game by id'()
	{
		setup:
		def id = "Weqg5Ti6"

		when:
		def game = client.fetchGame(id)

		then:
		game == id
		new File(config.outputDir + "/lichess", "${id}.pgn").exists()
	}

	def 'log error when fetching non-existent id'()
	{
		setup:
		def id = "asdfasdfasdf"

		when:
		def game = client.fetchGame(id)

		then:
		game == null
		notThrown(Exception)
	}

	def 'log error when fetching null id'()
	{
		setup:
		def id = null

		when:
		def game = client.fetchGame(id)

		then:
		game == null
		notThrown(Exception)
	}

	def 'can fetch all game ids'()
	{
		expect:
		client.getAllGameIds().size() > 1 //ok, not a great test
	}

//	@Ignore
	def 'can fetch all games'()
	{
		expect:
		client.getAllGameIds().size() == client.fetchHistory().size()
	}

	def 'can fetch all games since a given id'()
	{
		setup:
		def id = "ACV1BmUV"

		when:
		def games = client.fetchHistory(id)

		then:
		!games.contains("Ov8bKT3N")
		new File(config.outputDir + "/lichess").listFiles().length > 0
	}

	def 'can fetch latest game'() {
		when:
		def game = client.fetchLatest()

		then:
		game != null
		new File(config.outputDir + "/lichess", "${game[0]}.pgn").exists()
	}

	def 'can fetch last few games'() {
		expect:
		client.fetchLatest(5).size() == 5
	}
}
