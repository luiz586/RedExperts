package com.example.redexperts;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.redexperts.HttpRequest.HttpRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends FragmentActivity implements HttpRequestResult{
	GoogleMap mGoogleMap;
	LatLng mMyBeginingPosition;
	Marker mMarker;
	String markerText = "";
	String imageUrl;
	Bitmap imageBitmap;
	ProgressDialog mProgressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		boolean googleServicesInstalled = checkGooglePlayServices();
		if(googleServicesInstalled){
			initializeMap();
		}
		else{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder
			.setMessage("Problemy z Google Play Services. Prawdopodobnie nie jest zainstalowane.")
			.show();
		}
		
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/** Sprawdza czy jest dostepne googlePlayServices **/
	private boolean checkGooglePlayServices(){
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // Jesli googlePlayServices jest dostepne
        if (ConnectionResult.SUCCESS == resultCode){
        	return true;
        }
        else{
        	return false;
        }
	}

	/** Laduje mape **/
	private void initializeMap(){
		if(mGoogleMap == null){
			mGoogleMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			
			if(mGoogleMap == null){
				Toast.makeText(getBaseContext(), "Nie mozna wyswietlić mapy", Toast.LENGTH_SHORT).show();
			}
			else{
				mGoogleMap.setMyLocationEnabled(true);
				mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);							
			}

		}
	}
	
	
	/** Obsługa kliknięcia na przycisk "Pokaż cel". Pobiera dane celu z url i pokazuje go na mapie**/
	public void showTarget(View view){
		if(isConnectingToInternet()){
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setMessage("Wyznaczanie celu...");
			mProgressDialog.setCancelable(true);
			mProgressDialog.show();
			
			HttpRequest request = new HttpRequest(this);
			request.execute(HttpRequest.jsonUrl);
		}
		else{
			Toast.makeText(this, "Brak polaczenia z Internetem", Toast.LENGTH_LONG).show();
		}
		
	}
	
	/** Sprawdza polaczenie z Internetem **/
    public boolean isConnectingToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
          if (connectivity != null)
          {
              NetworkInfo[] info = connectivity.getAllNetworkInfo();
              if (info != null)
                  for (int i = 0; i < info.length; i++)
                      if (info[i].getState() == NetworkInfo.State.CONNECTED)
                      {
                          return true;
                      }
  
          }
          return false;
    }

	/** Odebranie odpowiedzi http **/
	@Override
	public void onHttpRequestFinish(String result) {
		if(mProgressDialog != null){
			mProgressDialog.dismiss();
		}
		
		if(result != null){
			try {
				// Rozpakuj jsona
				JSONObject json = new JSONObject(result);
				
				JSONObject location = json.getJSONObject("location");
				Double latitude = location.getDouble("latitude");
				Double longitude = location.getDouble("longitude");
				
				markerText = json.getString("text");
				imageUrl = json.getString("image");
				
				// Pokaz lokalizacje z jsona na mapie
				showLocalizationAsMarker(latitude, longitude);
				
				// Ustaw chmurkę z tekstem
				mGoogleMap.setInfoWindowAdapter(new MyWindowInfoAdapter());
				
				// Pobierz obrazek z urla z jsona
				getImageFromUrl(imageUrl);
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		else{
			Toast.makeText(this, "Brak celu", Toast.LENGTH_SHORT).show();
		}		
	}

	/** Pokazuje na mapie odczytana z http pozycje **/
	private void showLocalizationAsMarker(Double latitude, Double longitude){
		if(mGoogleMap != null){
			LatLng latLng = new LatLng(latitude, longitude);
			
			mMarker = mGoogleMap.addMarker(new MarkerOptions()
						.position(latLng));
			
			calculateDistance(latLng);
		}		
	}
	
	
	/** Oblicza dystans do celu i wyswietla go **/
	private void calculateDistance(LatLng targetLatLng){
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		String provider = service.getBestProvider(criteria, false);
		Location location = service.getLastKnownLocation(provider);
		mMyBeginingPosition = new LatLng(location.getLatitude(),location.getLongitude());
		
		mGoogleMap.animateCamera(CameraUpdateFactory.
				newLatLngZoom(mMyBeginingPosition, 7.0f ));
		
		float distance[] = new float[4];
		distance[0] = -1;
		Location.distanceBetween(mMyBeginingPosition.latitude, mMyBeginingPosition.longitude, 
				targetLatLng.latitude, targetLatLng.longitude, distance);
		
		if(distance[0] != -1){
			float distanceInKm = distance[0]/1000.0f;
			String stringDistanceInKm = String.format("%.2f", distanceInKm);
			Toast.makeText(this, "Odleglosc do celu to: " + stringDistanceInKm + " km", Toast.LENGTH_LONG).show();
		}
	}
	
	private void getImageFromUrl(String imageUrl){
		
		new AsyncTask<String, Void, Bitmap>(){

			@Override
			protected Bitmap doInBackground(String... params) {
				InputStream inputStream = null;
				BufferedInputStream bufferedInputStream = null;
				Bitmap bmp = null;
				
				try {
					   URL url = new URL(params[0]);
					   URLConnection conn = url.openConnection();
					   conn.connect();
					   inputStream = conn.getInputStream();
					   bufferedInputStream = new BufferedInputStream( inputStream );
					   bmp = BitmapFactory.decodeStream( bufferedInputStream );
				}
				catch (MalformedURLException e) {
				} 
				catch (IOException e) {
				} 
				finally {
					try {
						inputStream.close();
						bufferedInputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}					
				}		
				return bmp;  
		    }

			@Override
			protected void onPostExecute(Bitmap image) {
				super.onPostExecute(image);
				setImage(image);
				// Ustaw chmurkę z tekstem i obrazkiem
				mGoogleMap.setInfoWindowAdapter(new MyWindowInfoAdapter());
			}
						

		}.execute(imageUrl);
	}
	
	private void setImage(Bitmap image){
		imageBitmap = image;
	}
	
	
	
	class MyWindowInfoAdapter implements InfoWindowAdapter{
		private final View myContentsView;
		
		public MyWindowInfoAdapter() {
			myContentsView = getLayoutInflater().inflate(R.layout.custom_info_layout, null);
		}

		@Override
		public View getInfoContents(Marker marker) {
			
			TextView tv = ((TextView)myContentsView.findViewById(R.id.custom_info_text));
			tv.setText(markerText);
			ImageView iV = ((ImageView)myContentsView.findViewById(R.id.custom_info_image));
			if(imageBitmap != null){
				iV.setImageBitmap(imageBitmap);
			}
			return myContentsView;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
