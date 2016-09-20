package barretta.chess

import barretta.utils.PropertyManager
import groovy.util.logging.Slf4j

@Slf4j
class Twic
{
	public static void main(String[] args)
	{
		def cli = new CliBuilder(usage: "twic_pgn_fetcher [options]", header: "Options:")
		cli.o(longOpt: "outputPgnDir", args: 1, argName: "dir", "location to output fetched PGNs")
		cli.i(longOpt: "url", args: 1, argName: "url", "twic location, defaults to 'http://theweekinchess.com'")
		cli.h(longOpt: "fetchHistory", 'flag to fetch all available zips')
		cli.n(longOpt: "fetchId", args: 1, argName: "id", "which ID'd zip file to fetch")
		cli.s(longOpt: "scidDoImport", "flag to enable SCID import of fetched PGNs")
		cli.b(longOpt: "scidBinDir", args: 1, argName: "dir", "location of SCID binaries")
		cli.d(longOpt: "scidDb", args: 1, argName: "db_location", "location of target SCID DB")
		cli.h(longOpt: "help", "displays usage")

		def options = cli.parse(args)
		if (options.h)
		{
			cli.usage()
			System.exit(0)
		}

		def optionsMap = mapifyAndValidateOptions(cli, options)

		//if we don't have any CLI values, pull from the properties file
		if (optionsMap.isEmpty())
		{
			def properties = new PropertyManager()
			optionsMap.putAll(properties.properties as Map)
			log.info("Using config from twic.properties")
		}

		//todo: merge CLI and properties file to allow limited CLI override of properties

		run(optionsMap)
	}

	/**
	 * "main" method
	 * @param optionsMap - config options
	 */
	private static def run(optionsMap)
	{
		//fetch via crawler that has been configured in an ugly way
		def crawler = new TwicCrawler()
		crawler.config = new PropertyManager()
		crawler.config.properties.putAll(optionsMap)
		def fetchedIds = crawler.run()

		//save into SCID db if we have anything to save and we're told to do so
		if (fetchedIds.size() > 0 && optionsMap.doScidImport)
		{
			doScidImport(fetchedIds, optionsMap)
		}
		System.exit(0)
	}

	/**
	 * do scid import, of course
	 * @param fetchedIds - list of ids we fetched
	 * @param optionsMap - config options
	 */
	private static def doScidImport(fetchedIds, optionsMap)
	{
		if (new File(optionsMap.scidBinDir as String).exists())
		{
			//run sc_import on each pgn we fetched
			fetchedIds.each {
				def cmd = "${optionsMap.scidBinDir}/sc_import ${optionsMap.scidDb} ${optionsMap.outputPgnDir}/${it}.pgn"
				log.info("running [$cmd]...")
				def exec = cmd.execute(["PATH=" + System.getenv("PATH") + ":" + optionsMap.scidBinDir], null)
				def err = new StringBuffer()
				def out = new StringBuffer()
				exec.consumeProcessOutput(out, err)
				exec.waitFor()
				if (!err.toString().isEmpty())
				{
					log.error(err.toString())
				}
				else
				{
					if (out.length() > 0)
					{
						log.info(out.toString())
					}
					log.info("...done")
				}
			}
		}
		else
		{
			log.debug("SCID binary path [${optionsMap.scidBinDir}] does not exist")
			System.exit(1)
		}
	}

	private static def mapifyAndValidateOptions(cli, options)
	{
		//build a map from the CLI options like [<longOpt name>: <value>]
		def optionsMap = cli.options.getOptions().findAll { options[it.opt] }.collectEntries([:]) {
			[(it.longOpt): options[it.opt]]
		}

		//set default values
		if (!optionsMap.url)
		{
			optionsMap.url == "http://theweekinchess.com"
		}

		//validate
		def exit = false
		if (!optionsMap.outputPgnDir)
		{
			log.error("must have an outputPgnDir set")
			exit = true
		}
		if (!optionsMap.fetchId && !optionsMap.fetchHistory)
		{
			log.error("either fetchId must have a value or fetchHistory must be true")
			exit = true
		}

		if (exit)
		{
			System.exit(1)
		}
		else
		{
			return optionsMap
		}
	}
}
