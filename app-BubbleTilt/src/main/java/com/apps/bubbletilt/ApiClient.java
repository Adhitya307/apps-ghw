package com.apps.bubbletilt;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://192.168.1.12/GHW/api-apps/public/api/btm/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // TAMBAHKAN METHOD INI
    public static BtmApiService getBtmApiService() {
        return getClient().create(BtmApiService.class);
    }
}