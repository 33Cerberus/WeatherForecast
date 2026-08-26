# Weather Forecast (Kotlin Code Challenge)

[![CI](https://github.com/33Cerberus/WeatherForecast/actions/workflows/ci.yml/badge.svg)](https://github.com/33Cerberus/WeatherForecast/actions/workflows/ci.yml)

A command-line tool that fetches tomorrow's weather forecast for four cities — Chisinau, Madrid, Kyiv, and Amsterdam — from [WeatherAPI.com](https://www.weatherapi.com/) and prints it as a table to STDOUT.

Written in Kotlin, built with Gradle, and talks to the API through Retrofit — all three bonus points from the challenge. Requests for all four cities run in parallel, and a failure for one city (bad key, unknown location, network error, malformed response) shows as `No data` in that row instead of crashing the run.

```
City         Date         Min C    Max C    Humidity%  Wind kph   Dir
Chisinau     2026-08-27   16.8     20.5     78         13.0       ENE
Madrid       2026-08-27   20.0     26.0     32         27.4       SW
Kyiv         2026-08-27   16.7     25.3     41         14.4       N
Amsterdam    2026-08-27   17.6     28.1     62         15.8       E
```

## Requirements

- JDK 21
- A free API key from [weatherapi.com](https://www.weatherapi.com/) (sign up, then copy the key from your dashboard)

## Running it

Provide the API key either via a `.env` file in the project root:

```
WEATHER_API_KEY=your_api_key_here
```

(see `.env.example`) or as an environment variable, set in the same shell session you'll run the app from:

```bash
export WEATHER_API_KEY=your_api_key_here
```

```powershell
$env:WEATHER_API_KEY = "your_api_key_here"
```

Then run:

```bash
./gradlew run
```

```powershell
.\gradlew.bat run
```

If the key is missing or blank, the program prints a clear message to stderr and exits with code `1` instead of failing with a stack trace.

## Tests

```bash
./gradlew test
```

15 tests across four areas:

- **Mapping** (`ForecastMapper`) — wind direction as mode, date matching, `No data` fallbacks, and that the table shows the API's normalized city name rather than the raw query string.
- **Date resolution** (`TargetDate`) — `resolveTomorrow` against a `Clock.fixed`, so the result doesn't depend on when the test actually runs.
- **Output formatting** (`TablePrinter`) — header layout and long-city-name truncation, verified by temporarily capturing `System.out`.
- **The network layer** (`WeatherApi`) — a real HTTP round trip against a local `MockWebServer`, once with a valid response and once with a malformed one, confirming that a broken response degrades to `No data` rather than crashing. This is the only layer with an actual dependency on the network, and the only test suite that touches Retrofit's wiring directly.

CI (`.github/workflows/ci.yml`) runs the full build and test suite on every push.

## Architecture

```
io.github.cerberus33.weather
├── Main.kt      entry point: reads the key, wires everything together, prints the table
├── api/          Retrofit interface, HTTP client setup, parallel per-city fetching
├── model/        @Serializable data classes mirroring the WeatherAPI JSON response
├── mapper/       turns a raw API response into a printable CityWeatherRow
├── output/       table formatting and printing
└── time/         resolves target dates from an injectable Clock
```

`mapper`, `output`, and `time` have no dependency on networking or I/O — that's what makes them testable with plain data, no mocks required. `api` is the only layer that touches the network, and it's the only one tested with a mock server rather than in-memory values.

`Main.kt` stays intentionally thin: it reads the key, picks which date(s) to fetch, and prints. All actual logic — what a row should say, how a date is resolved, how a table row is formatted — lives in the layer responsible for it.

## Design decisions

**A single target date, shared by all four cities — not one resolved per city's own timezone.** A user checking the forecast late in the evening might already have "tomorrow" cross into the next day in Kyiv but not yet in Madrid. Resolving the date per city would show Kyiv's *day after* tomorrow in that case — technically correct relative to Kyiv, but not what "tomorrow" meant when the user asked. A single date, anchored to wherever the program runs, is what a side-by-side comparison table needs: one consistent answer to "what should I expect everywhere tomorrow," not four independently correct but mutually inconsistent tomorrows. The forecast day is found by matching the API's `date` field by value, not by trusting a fixed array index — an early version that read `forecastday[1]` directly could silently point at the wrong day near a city's midnight boundary.

**The table doesn't literally pivot dates into column headers.** The brief asks for "dates as columns, cities as rows"; here, date is one more data column per row rather than a header each date's metrics are grouped under. With a single day requested by default this doesn't lose any information — but it's a deliberate simplification, not an oversight, and it's worth naming as the one place the literal spec isn't followed to the letter. `resolveDateRange`/`resolveWeekAhead` (see below) already produce one row per (city, date) pair for a multi-day request; a true pivot would need `output/` to group those rows by date into column blocks, which wasn't built since a single day doesn't need it.

**Date resolution is a small set of composable, tested functions, not one hardcoded calculation.** `resolveToday()`, `resolveTomorrow()`, and `resolveDateRange(start, days)` all sit on top of one `resolveDate(daysFromNow, clock)`, each verified against a `Clock.fixed` so the result doesn't depend on when the test happens to run. The practical payoff: switching `Main.kt` from "tomorrow" to a full week is a one-line change (`resolveWeekAhead()` instead of `listOf(resolveTomorrow())`) — nothing in the mapper, printer, or models needs to change, since they already operate per (city, date) pair. `resolveForecastDays()` then works out how many days to actually request from the API, with a named safety margin (not a bare magic number) covering the same kind of ±1 day drift the date-matching fix above guards against.

**JSON parsing uses `kotlinx.serialization`, with every model field non-null.** The first version used Gson, which fills a field via reflection regardless of whether the JSON actually has it — a missing field can silently produce `0.0` in a `Double` deep inside a "successful" response instead of failing where the mistake actually is. `kotlinx.serialization` enforces the non-null contract at parse time: a genuinely missing field throws immediately, and that exception is caught by the same per-city error handling already in place for network failures, degrading to `No data` instead of quietly corrupting a row.

**Each city runs in its own coroutine, and a failure there doesn't touch the others.** `fetchForecasts` wraps each request in its own `try/catch`, re-throwing `CancellationException` so structured concurrency still works if the whole run is ever cancelled, and logging the real cause to stderr for anything else. `WeatherClient` implements `AutoCloseable` and is used with `use { }`, so OkHttp's background thread pool — which otherwise keeps the JVM alive for a while after the table is printed — shuts down deterministically, including if something upstream throws.

**Wind speed shown as the day's maximum, wind direction as the most common hourly value; humidity as the API's own daily average, rounded to a whole number.** An average wind speed can hide a strong evening gust behind a calm morning; the API already exposes a daily maximum, so no extra computation is needed. Direction has no daily field at all, only per-hour values — the most frequent one across the day is a more representative summary than picking a single arbitrary hour. Humidity, unlike temperature, doesn't need decimal precision to be meaningful, so it's the one column formatted with zero decimal places.

One thing intentionally left as-is: the city list is still hardcoded in `Main.kt` rather than taken as a CLI argument. Reasonable for a fixed four-city challenge; nothing else in the pipeline would need to change if that became configurable.