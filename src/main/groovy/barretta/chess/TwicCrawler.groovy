package barretta.chess

import barretta.utils.GroovyZipInputStream
import barretta.utils.PropertyManager
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.ccil.cowan.tagsoup.Parser

@Slf4j
class TwicCrawler
{
	PropertyManager config

	public static void main(String[] args)
	{
		new TwicCrawler().run()
	}

	/**
	 * "main" method
	 * @return
	 */
	def run()
	{
		loadConfig()
		log.info("Starting...will write to [${config.properties.outputPgnDir}]")

		def fetchedList = []
		if (config.properties.fetchHistory)
		{
			fetchedList += fetchHistory()
		}
		else
		{
			fetchedList << fetchId()
		}

		log.info("Done")
		return fetchedList
	}

	/**
	 * fetch a single file
	 * @return
	 */
	def fetchId()
	{
		def nextId = config.properties.fetchId ?: findLastId() //get the configured ID or the last present on the server
		try
		{
			writePgns(fetchAndExtractPgns(nextId), nextId)
			saveLastId(nextId + 1)
		}
		catch (IOException e)
		{
			log.error("Unable to find file with id [$nextId]")
		}
		return nextId
	}

	/**
	 * fetch all available files
	 * @return
	 */
	def fetchHistory()
	{
		def fetchedList = []        //holder of the IDs we successfully fetched
		def firstId = findFirstId() //find the min numbered ID on the server and start there
		def lastId = findLastId()   //max ID on server
		def count = 0

		//loop until we've fetched each file between first and last
		while (count < (lastId - firstId))
		{
			def id = firstId + count    //current id to fetch
			writePgns(fetchAndExtractPgns(id), id)
			fetchedList << id
			count++
		}
		//save what would be the next ID in the sequence as the new fetchId for the next run
		saveLastId(lastId + 1)

		log.info("Pulled [$count] zips")
		return fetchedList
	}

	/**
	 * get zip from the server and extract the PGNs from the zip
	 * @param id - id of file
	 * @return extracted entries
	 */
	def fetchAndExtractPgns(id)
	{
		def address = "${config.properties.url}/zips/twic${id}g.zip"
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

	/**
	 * get max file id from server
	 * @return id
	 */
	def findLastId()
	{
		def lastId = getZipList().max()
		log.info("Found latest file id [$lastId]")
		return lastId
	}

	/**
	 * get min file id from server
	 * @return id
	 */
	def findFirstId()
	{
		def firstId = getZipList().min()
		log.info("Found earliest file id [$firstId]")
		return firstId
	}

	/**
	 * get list of all the files available on TWIC for download...very dependent on HTML structure of the page
	 * NB: method is memoized to cache the results of the first call
	 * @return list of file IDs
	 */
	@Memoized
	def getZipList()
	{
		def html = new XmlSlurper(new Parser()).parse(new URL("$config.properties.url/twic").openStream())
		return html.body.depthFirst().find { it.@class == 'results-table' }.tbody.tr[2..-1].collect { it.td[5].a.@href }.inject([]) { list, file ->
			file = file.text().trim()
			def matcher = file =~ /\d+/
			if (matcher.find())
			{
				list << (matcher.group() as int)
			}
			return list
		}
	}

	/**
	 * write supplied PGNs from zip into a single PGN file on disk
	 * @param pgns - zip entries
	 * @param id - zip file id to be used in output file name
	 */
	def writePgns(pgns, id)
	{
		def outputFile = new File(config.properties.outputPgnDir, "${id}.pgn")
		outputFile.write(pgns.join("\n"))
		log.info("Wrote [$outputFile.absolutePath")
	}

	/**
	 * set the fetchId to the supplied value and write the config back to disk
	 * @param id - new "start" id for next run
	 */
	def saveLastId(id)
	{
		config.properties.fetchId = id
		config.save()
	}

	/**
	 * load twic.properties, do some validation
	 */
	private def loadConfig()
	{
		if (config?.properties.isEmpty())
		{
			config = new PropertyManager()
		}
		assert config.properties.url, "url must be set"
		assert config.properties.outputPgnDir, "ouptutPgnDir must be set"

		if (!new File(config.properties.outputPgnDir).exists())
		{
			log.info("output location [$config.properties.outputPgnDir] does not exist - creating")
			new File(config.properties.outputPgnDir).mkdirs()
		}
	}
}
