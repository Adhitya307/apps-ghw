package com.example.app.exstenso;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";

    // Ganti dengan URL API Anda yang sebenarnya
    public static final String BASE_URL = "http://192.168.1.11/GHW/api-apps/public/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Setup logging interceptor untuk debug
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Setup OkHttp client dengan timeout
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);

            httpClient.addInterceptor(logging);

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            android.util.Log.d(TAG, "✅ Retrofit client initialized with base URL: " + BASE_URL);
        }
        return retrofit;
    }

    public static ExstensoApiService getApiService() {
        return getClient().create(ExstensoApiService.class);
    }
}