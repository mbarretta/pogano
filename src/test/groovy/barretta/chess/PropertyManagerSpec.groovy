package barretta.chess

import barretta.chess.clients.LichessClient
import barretta.utils.PropertyManager
import spock.lang.Shared
import spock.lang.Specification

class PropertyManagerSpec extends Specification
{
	@Shared
	def testProperties = "poganoTest.properties"

	def setupSpec() {
		PropertyManager.instance.reload(testProperties)
	}

	def 'can find properties'()
	{
		expect:
		PropertyManager.instance.properties != null
	}

	def 'can reload properties'()
	{
		when:
		PropertyManager.instance.properties.lichessUrl = null
		PropertyManager.instance.reload(testProperties)

		then:
		PropertyManager.instance.properties.lichessUrl != null
	}

	def 'saves only last fetched Id by client and no other part of the properties'()
	{
		when:
		PropertyManager.instance.properties.fetchAll = true
		PropertyManager.instance.setLastFetchedId("123", new LichessClient())
		PropertyManager.instance.reload(testProperties)

		then:
		PropertyManager.instance.properties.LichessClient.lastFetchedId == 123

		!PropertyManager.instance.properties.containsKey("fetchAll")
	}
}
