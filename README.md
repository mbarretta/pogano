The Week In Chess PGN Fetcher (TPF)
================

This tool fetches PGN files from [The Week in Chess](http://theweekinchess.com) to a local directory and optionally into a [SCID](https://en.wikipedia
.org/wiki/Shane%27s_Chess_Information_Database)
database

##Building##
Gradle is the build tool of choice, leveraging the Application plugin to build a nice distribution directory.  Invoke thusly:

    gradle installApp

This will result in a distribution folder located at:

	<TPB_HOME>/build/install/twic_pgn_builder

You can run directly from there or move the directory at your pleasure.

###Configuring##
The main config file is `twic.properties`, which is located in `src/dist` or the root directory of the distribution folder (`build/install/twic_pgn_fetcher`) after building:

<pre>
//url of the twic zip directory
zipsUrl='http://theweekinchess.com/zips'

//directory to write fetched PGNs
pgnFile='/chess/games/twic'

//if true, TPF will get all available PGN files.
fetchHistory=false

//specific id to fetch.  if null, TPF will try to find the most recent id and fetch it.
//if fetchHistory=true, then this value is ignored
fetchId=1019

//if true, try and import fetched files into a SCID DB
doScidImport=true

//directory of SCID binaries
scidBinDir='/Applications/ScidvsMac.app/Contents/MacOS'

//SCID DB
scidDb='/chess/dbs/twic'
</pre>

##Running##
After building and configuring, do:

    $> bin/twic_pgn_fetcher[.bat]

When execution is complete, the next id (one plus the last successfully pulled id) will be saved to the `fetchId` property in `twic.properties` so it'll be ready to go for the
next run.

You can also run with CLI args:

    $> bin/twic_pgn_fetcher[.bat] --help

##Scheduleing##
So you'd like to have TPF run regularly? Well, I can't help you much beyond saying use cron or the Windows scheduler.

