package com.example.redexperts;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.os.AsyncTask;


public class HttpRequest extends AsyncTask<String, Void, String>{
	
	static final String jsonUrl = "https://dl.dropboxusercontent.com/u/6556265/test.json";
	HttpRequestResult delegate;
	
	public HttpRequest(){
		
	}
	public HttpRequest(HttpRequestResult delegate){
		this.delegate = delegate;
	}
	
	public interface HttpRequestResult{
		abstract void onHttpRequestFinish(String result);
	}

	@Override
	protected String doInBackground(String... params) {
		String response = makeHttpRequest(params[0]);
		return response;
	}
	

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		delegate.onHttpRequestFinish(result);
	}


	/** Zapytanie Http **/
	private String makeHttpRequest(String stringUrl){
		String response = null;
		URL url = createURL(stringUrl);
		
		if(url != null){	
			HttpURLConnection urlConnection = null;
			try {
				urlConnection = (HttpURLConnection)url.openConnection();
				InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
				response = readStream(inputStream);
			} 
			catch (IOException e) {
				e.printStackTrace();
			} 
			finally{
				if(url != null){
					urlConnection.disconnect();
				}
			}			
		}
		
		return response;
	}
	
	/** Tworzy URL na podstawie przekazanego Stringa **/
	private URL createURL(String stringUrl){
		URL url = null;
		try {
			url = new URL(stringUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("invalid url: " + stringUrl);
		}	
		
		return url;
	}
	
	/** Odczytuje i zwraca zawartość strumienia **/
	private String readStream(InputStream inputStream){
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
    	StringBuilder sb = new StringBuilder();
    	String line = null;
    	try {
			while( (line = reader.readLine()) != null){
				sb.append(line);
			}
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}  
    	
    	return sb.toString();
	}
	
}
