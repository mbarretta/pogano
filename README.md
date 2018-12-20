Pogano: the very limited PGN fetcher
================
This tool fetches PGN files from [The Week in Chess](http://theweekinchess.com) and [lichess](https://lichess.org) to a local directory and optionally into a [SCID](https://en.wikipedia.org/wiki/Shane%27s_Chess_Information_Database)
database

## Building
Gradle is the build tool of choice, leveraging the Application plugin to build a nice distribution directory.  Invoke thusly:

    gradle installDist

This will result in a distribution folder located at:

	<POGANO_HOME>/build/install/pogano

You can run directly from there or move the directory at your pleasure.

## Configuring
The main config file is `pogano.properties`, which is located in `src/dist` and copied to the root directory of the install folder (`build/install/pogano`) after building

## Running
After building and configuring, do:

    $> bin/pogano[.bat]

You can also run with CLI args:

    $> bin/pogano[.bat] --help
    
The primary use case is to run this once to fetch all current history (`--fetchAll --allHistory`) and then to subsequently run `--allSince` in order to get the latest games in either/both TWIC and lichess. The support this, the last fetched id will be saved to the `lastFetchId` property in `pogano.properties` after each run.

## Scheduleing
So you'd like to have pogano run regularly? Well, I can't help you much beyond saying use cron or the Windows scheduler.
