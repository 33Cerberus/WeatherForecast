# Weather Forecast (Kotlin Code Challenge)

Fetches tomorrow's weather forecast for four cities — Chisinau, Madrid, Kyiv, and Amsterdam — from [WeatherAPI.com](https://www.weatherapi.com/) and prints it as a table.

Built in Kotlin using Gradle and Retrofit (all three challenge bonus points).

[![CI](https://github.com/33Cerberus/WeatherForecast/actions/workflows/ci.yml/badge.svg)](https://github.com/33Cerberus/WeatherForecast/actions/workflows/ci.yml)

## Example output

```
City         Date         Min C    Max C    Humidity%  Wind kph   Dir
Chisinau     2026-08-26   16.8     20.5     78         13.0       ENE
Madrid       2026-08-26   20.0     26.0     32         27.4       SW
Kyiv         2026-08-26   16.7     25.3     41         14.4       N
Amsterdam    2026-08-26   17.6     28.1     62         15.8       E
```

If a city's forecast can't be fetched or doesn't contain the target date, its row shows `No data` instead of crashing the whole run.

## Requirements

- JDK 21
- A free API key from [weatherapi.com](https://www.weatherapi.com/) (sign up, then copy the key from your dashboard)

## Running it

Provide the API key either via a `.env` file in the project root:

```
WEATHER_API_KEY=your_api_key_here
```

(see `.env.example`) or as an environment variable, in the same shell session you'll run the app from:

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

On Windows:

```powershell
.\gradlew.bat run
```

If the key is missing or blank, the program prints a clear error to stderr and exits with code `1` instead of failing with a stack trace.

## Running the tests

```bash
./gradlew test
```

A GitHub Actions workflow (`.github/workflows/ci.yml`) runs the full build and test suite on every push.

## Architecture

```
io.github.cerberus33.weather
├── Main.kt              entry point: reads the key, wires everything together, prints the table
├── api/                 Retrofit interface, HTTP client setup, parallel fetching
├── model/                @Serializable data classes mirroring the WeatherAPI JSON response
├── mapper/               turns a raw API response into a printable CityWeatherRow
├── output/               table formatting and printing
└── time/                 resolves "tomorrow" from an injectable Clock
```

Each layer only depends on the ones below it. `mapper`, `output`, and `time` have no dependency on networking or I/O, which is what makes them unit-testable without mocks — `mapper` and `time` are tested against plain data, and `output` is tested by temporarily redirecting `System.out`.

## Design decisions

**Wind speed shown as the day's maximum, not the average.** An average can hide a strong evening gust behind a calm morning, understating real conditions. The API already exposes a daily maximum, so no extra computation is needed.

**Wind direction is the mode across all 24 hours, not a single snapshot.** The API's `day` object has no daily wind direction field, only per-hour values. Picking one hour (e.g. noon) would be an arbitrary sample; the most frequent direction across the day is a more representative summary. Ties are broken by whichever direction appears earliest in the hour list.

**"Tomorrow" is resolved once, from the machine's clock, deliberately not per city.** Consider a user in Madrid checking the forecast late in the evening on August 25th: Kyiv has already crossed into August 26th locally. Resolving "tomorrow" separately for each city's own timezone would show Kyiv's August 27th — technically "tomorrow relative to Kyiv," but not what the user meant when they asked for tomorrow's weather. A single shared target date, anchored to wherever the program is run, is what a comparison table across cities actually needs; the table is meant to answer "what should I expect everywhere tomorrow," not four independent, mutually inconsistent "tomorrows."

An earlier version selected the forecast by a fixed array index (`forecastday[1]`), which broke this same idea in a different way: near midnight, the API's own day boundary for a given city could shift `forecastday[1]` to the wrong date without the code noticing, since it never checked what date it actually got. The fix was to compute the target date once, then search the response for a `ForecastDay` whose `date` field matches it by value rather than trusting its position in the list. `days=3` is requested (not 2) to keep a safety margin against that kind of ±1 day drift. Date resolution lives in its own function, `resolveTargetDate(clock: Clock)`, tested independently of any network call with `Clock.fixed`.

**The displayed city name comes from the API's `location.name`, not the query string**, so a request like `"kiev"` shows up in the table as the normalized `"Kyiv"`. If the request failed outright, the original query string is kept instead, since no API-confirmed name exists.

**The OkHttp client is closed explicitly.** OkHttp keeps a non-daemon thread pool alive after the main logic finishes, which used to make the JVM hang for a while after printing the table. `WeatherClient` implements `AutoCloseable` and is used with Kotlin's `use { }`, so the client shuts down deterministically, including if an exception is thrown.

**A failed city doesn't stop the others.** Each city's request runs in its own coroutine; a failure (bad key, unknown city, network timeout) is caught, logged to stderr with the actual cause, and rendered as `No data` for that row only. `CancellationException` is re-thrown rather than swallowed, so structured concurrency (e.g. the whole run being cancelled) still works correctly. A partial failure still exits with code `0`, by design: the program did everything it could and produced a best-effort table rather than treating one bad city as a fatal error for the whole run.

**JSON parsing uses `kotlinx.serialization`, not Gson.** All response models are `@Serializable` with non-null fields. If the API response is ever missing a field the code expects, deserialization throws immediately at the parsing boundary instead of silently defaulting to `0.0` or `null` deep inside a non-null field — and that exception is caught by the same per-city error handling already in place, degrading to `No data` rather than corrupting a row. `ignoreUnknownKeys = true` is set explicitly, since the real API response includes many fields (`current`, `astro`, `alerts`, etc.) that aren't modeled here.