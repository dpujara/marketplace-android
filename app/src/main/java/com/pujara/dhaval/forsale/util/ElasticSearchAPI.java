package com.pujara.dhaval.forsale.util;

import com.pujara.dhaval.forsale.models.HitsObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Query;

public interface ElasticSearchAPI {
    @GET("_search/")
    Call<HitsObject> search(
            @HeaderMap Map<String,String> headers,
            @Query("default_operator") String operator, //1st query will automatically prepends ?
            @Query("q") String query //2nd query prepends & symbol. After 1st query all queries will prepend & symbol
            );
}
