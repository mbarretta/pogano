TWIC PGN Builder (TPB)
================

This tool builds or appends "The Week in Chess" pgn files to a local pgn file

##Building##
Gradle is the build tool of choice, leveraging the Application plugin to build a nice distribution directory.  Invoke thusly:

    gradle installApp

This will result in a distribution folder located at:

	<TPB_HOME>/build/install/twic_pgn_builder

You can run directly from there or move the directory at your pleasure.

###Configuring##
The main config file is `twic.properties`, which is located in `src/dist` or the root directory of the distribution folder:

<pre>
//url of the twic zip directory
zipsUrl='http://theweekinchess.com/zips'

//the pgn file in which to append twic pgns
pgnFile='/tmp/twic.pgn'

//if true, TPB will get all available pgn files.
fetchHistory=false

//specific id to fetch.  if null, TPB will try to find the most recent id and fetch it.
//if fetchHistory=true, then this value is ignored
fetchId='1019'
</pre>

##Running##
After building, do

    $> bin/twic_pgn_builder[.bat]

When execution is complete, the next id (one plus the last successfully pulled id) will be saved to the `fetchId` property in `twic.properties` so it'll be ready to go for the next run

##Scheduleing##
So you'd like to have TPB run regularly? Well, I can't help you much beyond saying use cron or the Windows scheduler.

