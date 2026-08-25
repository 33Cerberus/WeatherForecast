# Weather Forecast (Kotlin)

Fetches tomorrow's weather forecast for four cities — Chisinau, Madrid, Kyiv, and Amsterdam — from [WeatherAPI.com](https://www.weatherapi.com/) and prints it as a table.

Built in Kotlin using Gradle and Retrofit (all three challenge bonus points).

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

(see `.env.example`) or as an environment variable:

```bash
export WEATHER_API_KEY=your_api_key_here
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

## Architecture

```
io.github.cerberus33.weather
├── Main.kt              entry point: reads the key, wires everything together, prints the table
├── api/                 Retrofit interface, HTTP client setup, parallel fetching
├── model/                data classes mirroring the WeatherAPI JSON response
├── mapper/               turns a raw API response into a printable CityWeatherRow
├── output/               table formatting and printing
└── time/                 resolves "tomorrow" from an injectable Clock
```

Each layer only depends on the ones below it. `mapper` and `time` have no dependency on networking or I/O, which is what makes them unit-testable without mocks.

## Design decisions

**Wind speed shown as the day's maximum, not the average.** An average can hide a strong evening gust behind a calm morning, understating real conditions. The API already exposes a daily maximum, so no extra computation is needed.

**Wind direction is the mode across all 24 hours, not a single snapshot.** The API's `day` object has no daily wind direction field, only per-hour values. Picking one hour (e.g. noon) would be an arbitrary sample; the most frequent direction across the day is a more representative summary. Ties are broken by whichever direction appears earliest in the hour list.

**"Tomorrow" is resolved once, from the machine's clock, and matched by date value rather than by array index.** Early versions took `forecastday[1]` directly, which silently returned the wrong day for cities that had already crossed midnight in their own timezone relative to the machine running the program. The fix: compute one target date up front, then search the response for a `ForecastDay` whose `date` matches it. `days=3` is requested (not 2) to keep a margin against a ±1 day offset. The target date is exposed as a testable function (`resolveTargetDate`) that takes a `Clock`, though timezone-per-city resolution was intentionally not implemented — the table shows one consistent date for all four cities, which also keeps the "dates as columns" requirement trivially satisfied for a single-day forecast.

**All API response fields are nullable in the Kotlin models.** Gson populates DTOs via reflection and does not enforce Kotlin's non-null types — a missing JSON field silently produces `null` in a field declared non-null, which then fails somewhere downstream instead of at the parsing boundary. Modeling every field as nullable makes the real behavior explicit and lets `buildRow` degrade to `No data` instead of throwing.

**The displayed city name comes from the API's `location.name`, not the query string**, so a request like `"kiev"` shows up in the table as the normalized `"Kyiv"`. If the request failed outright, the original query string is kept instead, since no API-confirmed name exists.

**The OkHttp client is closed explicitly.** OkHttp keeps a non-daemon thread pool alive after the main logic finishes, which used to make the JVM hang for a while after printing the table. `WeatherClient` implements `AutoCloseable` and is used with Kotlin's `use { }`, so the client shuts down deterministically, including if an exception is thrown.

**Retrofit, Gson, and OkHttp are declared as direct dependencies**, not left to whatever version Gradle resolves transitively — partly for reproducibility, and partly because the transitively-resolved OkHttp version had a known CVE that pinning `4.12.0` resolves.

## Known trade-offs

- Gson is used for JSON parsing. It works but doesn't respect Kotlin nullability at the type level (mitigated above by making all fields nullable and testing the mapping with `deserializes the WeatherAPI response shape`). `kotlinx.serialization` would be the more idiomatic choice for a production Kotlin project.
- Each city's "tomorrow" is resolved against the machine's local clock rather than the city's own timezone. Given the four fixed cities are all within one hour of each other, this doesn't produce inconsistent dates in practice, but it would need revisiting for cities further apart.
