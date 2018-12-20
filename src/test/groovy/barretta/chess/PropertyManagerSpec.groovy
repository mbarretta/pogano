package barretta.chess

import barretta.chess.clients.LichessClient
import barretta.utils.PropertyManager
import spock.lang.Specification

class PropertyManagerSpec extends Specification
{
	def 'can find pogano.properties'()
	{
		expect:
		PropertyManager.instance.properties != null
	}

	def 'can reload properties'()
	{
		when:
		PropertyManager.instance.properties.lichessUrl = null
		PropertyManager.instance.reload()

		then:
		PropertyManager.instance.properties.lichessUrl != null
	}

	def 'saves only last fetched Id by client and no other part of the properties'()
	{
		when:
		PropertyManager.instance.properties.fetchAll = true
		PropertyManager.instance.setLastFetchedId("123", new LichessClient())
		PropertyManager.instance.reload()

		then:
		PropertyManager.instance.properties.LichessClient.lastId == 123

		!PropertyManager.instance.properties.containsKey("fetchAll")
	}
}
