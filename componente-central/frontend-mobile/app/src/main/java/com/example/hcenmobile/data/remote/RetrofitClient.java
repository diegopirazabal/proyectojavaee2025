package com.example.hcenmobile.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.hcenmobile.util.Constants;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente Retrofit para comunicación HTTP con el backend
 * Incluye inyección del header Authorization con el JWT guardado en SharedPreferences.
 */
public class RetrofitClient {

    private static RetrofitClient instance;
    private final Retrofit retrofit;
    private final Context appContext;

    private RetrofitClient(Context context) {
        this.appContext = context.getApplicationContext();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = getUnsafeOkHttpClient()
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(this::applyAuthHeader)
                .addInterceptor(loggingInterceptor)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            if (context == null) {
                throw new IllegalStateException("Context requerido para inicializar RetrofitClient");
            }
            instance = new RetrofitClient(context);
        }
        return instance;
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RetrofitClient no inicializado. Llame a getInstance(context) primero.");
        }
        return instance;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }

    private Response applyAuthHeader(Interceptor.Chain chain) throws IOException {
        Request original = chain.request();
        SharedPreferences prefs = appContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String jwt = prefs.getString(Constants.PREF_JWT_TOKEN, null);

        if (!TextUtils.isEmpty(jwt)) {
            Request authorized = original.newBuilder()
                    .addHeader("Authorization", "Bearer " + jwt)
                    .build();
            return chain.proceed(authorized);
        }

        return chain.proceed(original);
    }

    /**
     * Crea un OkHttpClient que acepta todos los certificados SSL.
     * ADVERTENCIA: Solo usar en desarrollo. NO usar en producción.
     */
    private static OkHttpClient.Builder getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            return builder;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
