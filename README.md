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

21 tests across four areas:

- **Mapping** (`ForecastMapper`) — wind direction as mode, date matching, `No data` fallbacks, and that the table shows the API's normalized city name rather than the raw query string.
- **Date resolution** (`TargetDate`) — `resolveTomorrow` against a `Clock.fixed`, so the result doesn't depend on when the test actually runs.
- **Output formatting** (`TablePrinter`) — header layout and long-city-name truncation, verified by temporarily capturing `System.out`.
- **The network layer** (`WeatherApi`) — a real HTTP round trip against a local `MockWebServer`, once with a valid response and once with a malformed one, confirming that a broken response degrades to `No data` rather than crashing. This is the only layer with an actual dependency on the network, and the only test suite that touches Retrofit's wiring directly.

CI (`.github/workflows/ci.yml`) runs the full build and test suite on every push.

## Architecture

```
io.github.cerberus33.weather
├── Main.kt       entry point: reads the key, wires everything together, prints the table
├── api/          Retrofit interface, HTTP client setup, parallel per-city fetching
├── model/        @Serializable data classes mirroring the WeatherAPI JSON response
├── mapper/       turns a raw API response into a printable CityWeatherRow
├── output/       table formatting and printing
└── time/         resolves target dates from an injectable Clock
```

`mapper`, `output`, and `time` have no dependency on networking or I/O — that's what makes them testable with plain data, no mocks required. `api` is the only layer that touches the network, and it's the only one tested with a mock server rather than in-memory values.

`Main.kt` stays intentionally thin: it reads the key, picks which date(s) to fetch, and prints. All actual logic — what a row should say, how a date is resolved, how a table row is formatted — lives in the layer responsible for it.

## Design decisions

**A single target date, shared by all four cities, matched by value rather than by array index.** An early version read `forecastday[1]` directly, which could silently point at the wrong day near a city's own midnight boundary; the fix was to compute one target date up front and search the response for a `ForecastDay` whose `date` field actually matches it. The date itself is deliberately *not* resolved per city's timezone — a user checking the forecast late in the evening might already have "tomorrow" cross into the next day in Kyiv but not in Madrid, and a per-city date would show Kyiv's day-after-tomorrow in that case, correct relative to Kyiv but not what the user meant by "tomorrow." One consequence worth naming directly: the brief asks for "dates as columns, cities as rows," and this table shows date as a per-row column instead of a pivoted header — a deliberate simplification for a single-day forecast (`resolveDateRange` already produces one row per city/date pair, so the underlying data supports more than one day; the table just doesn't group them into column blocks, since one day doesn't need it).

**JSON parsing uses `kotlinx.serialization`, with every model field non-null.** The first version used Gson, which fills a field via reflection regardless of whether the JSON actually has it — a missing field can silently produce `0.0` in a `Double` deep inside a "successful" response instead of failing where the mistake actually is. `kotlinx.serialization` enforces the non-null contract at parse time: a genuinely missing field throws immediately, and that exception is caught by the same per-city error handling already in place for network failures, degrading to `No data` instead of quietly corrupting a row.

**Each city runs in its own coroutine, and a failure there doesn't touch the others.** `fetchForecasts` wraps each request in its own `try/catch`, re-throwing `CancellationException` so structured concurrency still works if the whole run is ever cancelled, and logging the real cause to stderr for anything else. `WeatherClient` implements `AutoCloseable` and is used with `use { }`, so OkHttp's background thread pool — which otherwise keeps the JVM alive for a while after the table is printed — shuts down deterministically, including if something upstream throws.

**Wind speed is the day's maximum and wind direction is the most common hourly value, not an average or a single snapshot.** An average speed can hide a strong evening gust behind a calm morning, and the API has no daily direction field at all, only per-hour ones — the most frequent hourly direction is a more honest summary than picking one arbitrary hour. Humidity is printed with no decimal places, since fractional percent points aren't meaningful information.

One thing intentionally left as-is: the city list is hardcoded in `Main.kt` rather than taken as a CLI argument — reasonable for a fixed four-city challenge.
