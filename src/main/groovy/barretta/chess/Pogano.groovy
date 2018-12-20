package barretta.chess

import barretta.chess.clients.LichessClient
import barretta.chess.clients.TwicClient
import barretta.utils.PropertyManager
import groovy.util.logging.Slf4j

@Slf4j
class Pogano {
    static def config

    static interface PoganoClient {
        List run()
    }

    static void main(String[] args) {
        config = PropertyManager.instance.properties

        def cli = new CliBuilder(usage: "pogano [commands] [options] [config]", header: "Options:")
        cli.o(longOpt: "outputDir", args: 1, argName: "dir", "location to output fetched PGNs - otherwise writes to stdout")
        cli._(longOpt: "fetchTwic", "[command] fetch TWIC games")
        cli._(longOpt: "fetchLichess", "[command] fetch lichess games")
        cli._(longOpt: "fetchAll", "[command] fetch TWIC and lichess games: default")
        cli._(longOpt: "importToScid", "[command] flag to enable SCID import of fetched PGNs using configured SCID DBs")
        cli._(longOpt: "allHistory", "[option] flag to fetch all available games")
        cli._(longOpt: "allSince", "[option] flag to fetch available games since last fetch")
        cli._(longOpt: "latest", "[option] flag to fetch the most recent games")
        cli._(longOpt: "count", "[option] number of most recent games to fetch - used with --latest [default: 1]")
        cli._(longOpt: "id", args: 1, argName: "id", "[option] specific game/zip ID to fetch")
        cli._(longOpt: "scidDb", args: 1, argName: "db", "[config] location of target SCID DB - will override ALL configured SCID DBs and will cause import of ALL fetched PGNs " +
            "into designated DB")
        cli._(longOpt: "lichessUser", args: 1, argName: "user", "[config] lichess username")
        cli._(longOpt: "lichessApiToken", args: 1, argName: "token", "[config] lichess API token")
        cli.h(longOpt: "help", "displays usage")

        def options = cli.parse(args)
        def optionsMap = [:]

        //if we have cli options, overwrite default values
        if (options && args) {
            if (options.h) {
                cli.usage()
                System.exit(0)
            }

            optionsMap += mapifyOptions(cli, options)

            //merge the properties file values with those from CLI with the exception of the "fetch" commands -
            //we want to be able to run just one fetch operation from CLI even if the properties file has fetchAll=true
            config.findAll { it.key.startsWith("fetch") }.each {
                config.put(it.key, false)
            }
            config.putAll(optionsMap)
        }

        run()
    }

    /**
     * "main" method
     */
    private static def run() {
        if (PropertyManager.instance.isValid()) {
            log.info("running with options: \n ${PropertyManager.instance.toString()}")

            if (config.fetchTwic || config.fetchAll) {
                log.info("fetching from twic")
                doScidImport(new TwicClient().run(), "twic")
            }

            if (config.fetchLichess || config.fetchAll) {
                log.info("fetching from lichess")
                doScidImport(new LichessClient().run(), "lichess")
            }

            System.exit(0)
        } else {
            System.exit(1)
        }
    }

    /**
     * do scid import, of course
     * @param games - list of games we fetched
     * @param suffix - folder where these ids live under the primary output directory
     */
    private static def doScidImport(games, suffix) {
        if (!config.importToScid) {
            return
        }

        def scidBin = config.scidBinDir
        def db = config.scidDb ?: (suffix == "twic" ? config.twicScidDb : config.lichessScidDb)

        //error checking
        if (!new File(scidBin).exists()) {
            log.debug("SCID binary path [$scidBin] does not exist")
            return
        } else if (games.isEmpty()) {
            log.warn("no IDs fetched for [$suffix]")
            return
        } else if (!new File("${db}.si4").exists()) {
            log.error("SCID DB [${db}.si4] does not exist")
            return
        }

        //run sc_import on each pgn we fetched
        log.info("importing game(s) into SCID")
        games.each {
            def cmd = "$scidBin/sc_import $db ${config.outputDir}/$suffix/${it}.pgn"
            log.info("running [$cmd]...")
            def exec = cmd.execute(["PATH=" + System.getenv("PATH") + ":" + scidBin], null)
            def err = new StringBuffer()
            def out = new StringBuffer()
            exec.consumeProcessOutput(out, err)
            exec.waitFor()
            if (!err.toString().isEmpty()) {
                log.error(err.toString())
            } else {
                if (out.length() > 0) {
                    log.info(out.toString())
                }
                log.info("...done")
            }
        }
    }

    private static def mapifyOptions(cli, options) {
        //build a map from the CLI options like [<longOpt name>: <value>]
        return cli.options.getOptions().findAll { options[it.longOpt] }.collectEntries([:]) {
            [(it.longOpt): options[it.longOpt]]
        }
    }
}
