import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast.json")
    suspend fun getForecast(
        @Query("q") q: String,
        @Query("days") days: Int,
        @Query("key") key: String
    ): ForecastResponse
}